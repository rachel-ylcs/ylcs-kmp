#include "linux_gstreamer.h"
#include <algorithm>
#include <atomic>
#include <cstring>
#include <cerrno>
#include <cstdlib>
#include <mutex>
#include <thread>
#include <poll.h>
#include <sys/eventfd.h>
#include <unistd.h>

namespace {
    struct SampleLease {
        LinuxGstreamer &api;
        GstSample *value = nullptr;

        explicit SampleLease(LinuxGstreamer &backend) : api(backend) { }
        ~SampleLease() { if (value) api.gst_mini_object_unref(reinterpret_cast<GstMiniObject *>(value)); }
    };

    struct VideoState {
        LinuxGstreamer &api;
        std::shared_ptr<AudioNotifications> notifications;
        std::atomic_bool closed {false};
        std::atomic_bool playing {false};
        std::atomic_bool wantsPlay {false};
        std::atomic_bool ended {false};
        std::atomic_bool ready {false};
        std::atomic_bool pendingFrame {false};
        std::atomic_bool seeking {false};
        std::atomic<int64_t> seekTarget {0};
        std::atomic<int64_t> error {0};
        std::atomic<int64_t> duration {0};
        std::mutex frames;
        GstSample *sample = nullptr;
        int64_t serial = 0;

        VideoState(LinuxGstreamer &backend, std::shared_ptr<AudioNotifications> listener) : api(backend), notifications(std::move(listener)) { }
        ~VideoState() { if (sample) api.gst_mini_object_unref(reinterpret_cast<GstMiniObject *>(sample)); }

        GstFlowReturn receive(GstAppSink *sink, bool preroll) noexcept {
            try {
                auto *next = preroll ? api.gst_app_sink_pull_preroll(sink) : api.gst_app_sink_pull_sample(sink);
                if (!next) return closed.load() ? GST_FLOW_FLUSHING : GST_FLOW_EOS;
                GstSample *previous;
                {
                    std::lock_guard<std::mutex> guard(frames);
                    if (closed.load()) {
                        api.gst_mini_object_unref(reinterpret_cast<GstMiniObject *>(next));
                        return GST_FLOW_FLUSHING;
                    }
                    previous = sample;
                    sample = next;
                    ++serial;
                }
                if (previous) api.gst_mini_object_unref(reinterpret_cast<GstMiniObject *>(previous));
                if (!closed.load() && !pendingFrame.exchange(true)) notifications->emit(RACHEL_AUDIO_EVENT_FRAME);
                return GST_FLOW_OK;
            }
            catch (...) { return GST_FLOW_ERROR; }
        }
    };

    class LinuxVideoPlayer final : public RachelAudioBackend {
        LinuxGstreamer &api = LinuxGstreamer::instance();
        std::shared_ptr<VideoState> state;
        GstElement *player = nullptr;
        GstElement *sink = nullptr;
        GstBus *bus = nullptr;
        int wake = -1;
        std::thread messages;
        std::mutex operations;
        bool buffering = false;
        bool sourceSent = false;

        void setState(GstState value) {
            if (api.gst_element_set_state(player, value) == GST_STATE_CHANGE_FAILURE) throw RachelAudioError(-1, "Cannot change GStreamer playback state");
        }

        void updateDuration() {
            gint64 length = 0;
            if (api.gst_element_query_duration(player, GST_FORMAT_TIME, &length) && length >= 0) {
                const auto value = length / GST_MSECOND;
                if (state->duration.exchange(value) != value) state->notifications->emit(RACHEL_AUDIO_EVENT_DURATION, value);
            }
        }

        void applySeek() {
            const auto position = state->seekTarget.load();
            if (!api.gst_element_seek_simple(player, GST_FORMAT_TIME, static_cast<GstSeekFlags>(GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_ACCURATE), position * GST_MSECOND)) {
                state->seeking.store(false);
                throw RachelAudioError(-1, "Video source does not support seeking");
            }
        }

        void receiveMessage(GstMessage *message) {
            switch (GST_MESSAGE_TYPE(message)) {
                case GST_MESSAGE_ERROR: {
                    GError *failure = nullptr;
                    gchar *debug = nullptr;
                    api.gst_message_parse_error(message, &failure, &debug);
                    const auto code = failure && failure->code ? failure->code : -1;
                    state->error.store(code);
                    state->playing.store(false);
                    state->wantsPlay.store(false);
                    state->notifications->emit(RACHEL_AUDIO_EVENT_ERROR, code, failure ? failure->message : "GStreamer playback failed");
                    if (failure) api.g_error_free(failure);
                    if (debug) api.g_free(debug);
                    break;
                }
                case GST_MESSAGE_EOS:
                    state->playing.store(false);
                    state->wantsPlay.store(false);
                    if (!state->ended.exchange(true)) state->notifications->emit(RACHEL_AUDIO_EVENT_ENDED);
                    break;
                case GST_MESSAGE_STATE_CHANGED: {
                    if (GST_MESSAGE_SRC(message) != reinterpret_cast<GstObject *>(player)) break;
                    GstState before, current, pending;
                    api.gst_message_parse_state_changed(message, &before, &current, &pending);
                    state->playing.store(current == GST_STATE_PLAYING && !state->ended.load());
                    state->notifications->emit(RACHEL_AUDIO_EVENT_STATE, current);
                    break;
                }
                case GST_MESSAGE_ASYNC_DONE: {
                    if (GST_MESSAGE_SRC(message) != reinterpret_cast<GstObject *>(player)) break;
                    std::lock_guard<std::mutex> guard(operations);
                    updateDuration();
                    const bool wasReady = state->ready.exchange(true);
                    if (!wasReady && state->seeking.load()) applySeek();
                    else state->seeking.store(false);
                    if (!sourceSent) {
                        sourceSent = true;
                        state->notifications->emit(RACHEL_AUDIO_EVENT_SOURCE);
                    }
                    break;
                }
                case GST_MESSAGE_DURATION_CHANGED:
                    updateDuration();
                    break;
                case GST_MESSAGE_BUFFERING: {
                    gint percent = 0;
                    api.gst_message_parse_buffering(message, &percent);
                    std::lock_guard<std::mutex> guard(operations);
                    buffering = percent < 100;
                    if (state->wantsPlay.load()) setState(buffering ? GST_STATE_PAUSED : GST_STATE_PLAYING);
                    break;
                }
                default: break;
            }
        }
    public:
        explicit LinuxVideoPlayer(const std::shared_ptr<AudioNotifications> &notifications) : state(std::make_shared<VideoState>(api, notifications)) { }

        void load(const char *path) {
            player = api.gst_element_factory_make("playbin", nullptr);
            sink = api.gst_element_factory_make("appsink", nullptr);
            if (player) api.gst_object_ref_sink(player);
            if (sink) api.gst_object_ref_sink(sink);
            if (!player || !sink) throw RachelAudioError(-1, "GStreamer playbin/appsink plugins are not installed");
            auto *caps = api.gst_caps_from_string("video/x-raw,format=BGRA,pixel-aspect-ratio=1/1");
            if (!caps) throw RachelAudioError(-1, "Cannot create video caps");
            api.gst_app_sink_set_caps(reinterpret_cast<GstAppSink *>(sink), caps);
            api.gst_mini_object_unref(reinterpret_cast<GstMiniObject *>(caps));
            api.g_object_set(sink, "max-buffers", 1u, "drop", TRUE, "wait-on-eos", FALSE, "enable-last-sample", FALSE, "sync", TRUE, nullptr);
            GstAppSinkCallbacks callbacks {};
            callbacks.new_preroll = [](GstAppSink *output, gpointer data) {
                return (*static_cast<std::shared_ptr<VideoState> *>(data))->receive(output, true);
            };
            callbacks.new_sample = [](GstAppSink *output, gpointer data) {
                return (*static_cast<std::shared_ptr<VideoState> *>(data))->receive(output, false);
            };
            api.gst_app_sink_set_callbacks(reinterpret_cast<GstAppSink *>(sink), &callbacks, new std::shared_ptr<VideoState>(state), [](gpointer data) { delete static_cast<std::shared_ptr<VideoState> *>(data); });
            std::string uri = path;
            if (uri.find("://") == std::string::npos) {
                std::unique_ptr<char, decltype(&std::free)> resolved(realpath(path, nullptr), std::free);
                if (!resolved) throw RachelAudioError(errno, "Cannot resolve video filename");
                const std::string filename = resolved.get();
                GError *failure = nullptr;
                auto *value = api.gst_filename_to_uri(filename.c_str(), &failure);
                if (!value) {
                    const std::string reason = failure ? failure->message : "Invalid video filename";
                    if (failure) api.g_error_free(failure);
                    throw RachelAudioError(-1, reason);
                }
                uri = value;
                api.g_free(value);
            }
            api.g_object_set(player, "uri", uri.c_str(), "video-sink", sink, nullptr);
            bus = api.gst_element_get_bus(player);
            if (!bus) throw RachelAudioError(-1, "Cannot create video message bus");
            wake = eventfd(0, EFD_CLOEXEC | EFD_NONBLOCK);
            if (wake == -1) throw RachelAudioError(errno, "Cannot create video shutdown signal");
            messages = std::thread([this] {
                GPollFD source {};
                api.gst_bus_get_pollfd(bus, &source);
                pollfd descriptors[] {{source.fd, POLLIN, 0}, {wake, POLLIN, 0}};
                while (!state->closed.load()) {
                    const auto result = poll(descriptors, 2, -1);
                    if (result < 0) {
                        if (errno == EINTR) continue;
                        state->error.store(errno);
                        state->notifications->emit(RACHEL_AUDIO_EVENT_ERROR, errno, "Cannot wait for video events");
                        break;
                    }
                    if (state->closed.load() || descriptors[1].revents) break;
                    if (descriptors[0].revents & (POLLERR | POLLHUP | POLLNVAL)) break;
                    while (!state->closed.load()) {
                        auto *message = api.gst_bus_timed_pop(bus, 0);
                        if (!message) break;
                        try { receiveMessage(message); }
                        catch (const std::exception &failure) {
                            state->error.store(-1);
                            state->playing.store(false);
                            state->notifications->emit(RACHEL_AUDIO_EVENT_ERROR, -1, failure.what());
                        }
                        api.gst_mini_object_unref(reinterpret_cast<GstMiniObject *>(message));
                    }
                }
            });
            setState(GST_STATE_PAUSED);
        }

        ~LinuxVideoPlayer() override {
            state->closed.store(true);
            if (wake != -1) {
                const uint64_t signal = 1;
                while (write(wake, &signal, sizeof(signal)) == -1 && errno == EINTR) { }
            }
            if (messages.joinable()) messages.join();
            if (wake != -1) close(wake);
            if (bus) api.gst_bus_set_flushing(bus, TRUE);
            if (sink) {
                GstAppSinkCallbacks callbacks {};
                api.gst_app_sink_set_callbacks(reinterpret_cast<GstAppSink *>(sink), &callbacks, nullptr, nullptr);
            }
            GstSample *previous;
            {
                std::lock_guard<std::mutex> guard(state->frames);
                previous = state->sample;
                state->sample = nullptr;
            }
            if (previous) api.gst_mini_object_unref(reinterpret_cast<GstMiniObject *>(previous));
            if (player) api.gst_element_set_state(player, GST_STATE_NULL);
            if (sink) api.gst_object_unref(sink);
            if (bus) api.gst_object_unref(bus);
            if (player) api.gst_object_unref(player);
        }

        RachelAudioSnapshot query() override {
            gint64 position = 0;
            if (!state->seeking.load()) api.gst_element_query_position(player, GST_FORMAT_TIME, &position);
            const auto duration = state->duration.load();
            return {state->playing.load(), state->ended.load() ? duration : state->seeking.load() ? state->seekTarget.load() : std::max<int64_t>(position / GST_MSECOND, 0), duration, state->error.load()};
        }

        void play() override {
            std::lock_guard<std::mutex> guard(operations);
            if (state->ended.exchange(false)) {
                state->seekTarget.store(0);
                state->seeking.store(true);
                if (state->ready.load()) applySeek();
            }
            state->wantsPlay.store(true);
            setState(buffering ? GST_STATE_PAUSED : GST_STATE_PLAYING);
        }

        void pause() override {
            std::lock_guard<std::mutex> guard(operations);
            state->wantsPlay.store(false);
            state->playing.store(false);
            setState(GST_STATE_PAUSED);
        }

        void seek(int64_t position) override {
            std::lock_guard<std::mutex> guard(operations);
            const auto duration = state->duration.load();
            state->seekTarget.store(std::clamp<int64_t>(position, 0, duration > 0 ? duration : INT64_MAX / GST_MSECOND));
            state->seeking.store(true);
            state->ended.store(false);
            if (state->ready.load()) applySeek();
        }

        bool videoInfo(RachelVideoFrameInfo &info) override {
            std::lock_guard<std::mutex> guard(state->frames);
            state->pendingFrame.store(false);
            if (!state->sample) return false;
            GstVideoInfo format {};
            if (!api.gst_video_info_from_caps(&format, api.gst_sample_get_caps(state->sample))) return false;
            info = {GST_VIDEO_INFO_WIDTH(&format), GST_VIDEO_INFO_HEIGHT(&format), state->serial, nullptr};
            return info.width > 0 && info.height > 0 && info.width <= 16384 && info.height <= 16384 && static_cast<int64_t>(info.width) * info.height * 4 <= INT32_MAX;
        }

        bool copyVideoFrame(void *pixels, int64_t capacity, int width, int height) override {
            SampleLease sample(api);
            {
                std::lock_guard<std::mutex> guard(state->frames);
                if (state->sample) sample.value = reinterpret_cast<GstSample *>(api.gst_mini_object_ref(reinterpret_cast<GstMiniObject *>(state->sample)));
            }
            if (!sample.value || static_cast<int64_t>(width) * height * 4 > capacity) return false;
            GstVideoInfo format {};
            if (!api.gst_video_info_from_caps(&format, api.gst_sample_get_caps(sample.value)) || GST_VIDEO_INFO_WIDTH(&format) != width || GST_VIDEO_INFO_HEIGHT(&format) != height || GST_VIDEO_INFO_FORMAT(&format) != GST_VIDEO_FORMAT_BGRA) return false;
            GstVideoFrame frame {};
            if (!api.gst_video_frame_map(&frame, &format, api.gst_sample_get_buffer(sample.value), GST_MAP_READ)) return false;
            const auto stride = GST_VIDEO_FRAME_PLANE_STRIDE(&frame, 0);
            const auto rowBytes = static_cast<size_t>(width) * 4;
            const bool valid = stride >= static_cast<int64_t>(rowBytes) || static_cast<int64_t>(stride) <= -static_cast<int64_t>(rowBytes);
            if (valid) {
                const auto *source = static_cast<const uint8_t *>(GST_VIDEO_FRAME_PLANE_DATA(&frame, 0));
                if (stride == static_cast<int64_t>(rowBytes)) std::memcpy(pixels, source, rowBytes * height);
                else {
                    for (int y = 0; y < height; ++y) std::memcpy(static_cast<uint8_t *>(pixels) + y * rowBytes, source + static_cast<ptrdiff_t>(y) * stride, rowBytes);
                }
            }
            api.gst_video_frame_unmap(&frame);
            return valid;
        }
    };
}

std::shared_ptr<RachelAudioBackend> rachel_video_open(const char *path, const std::shared_ptr<AudioNotifications> &notifications) {
    auto result = std::make_shared<LinuxVideoPlayer>(notifications);
    result->load(path);
    return result;
}
