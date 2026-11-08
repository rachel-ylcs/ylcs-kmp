#include "rachel_audio_internal.h"
#include <miniaudio.h>
#include <algorithm>
#include <atomic>
#include <limits>
#include <mutex>
#include <thread>
#include <vector>
#ifdef _WIN32
#include <windows.h>
#endif

namespace {
    void check(ma_result result) {
        if (result != MA_SUCCESS) throw RachelAudioError(result);
    }

    struct AudioEngine {
        ma_engine value {};
#ifdef RACHEL_IOS_AUDIO
        ma_device device {};
        std::mutex outputMutex;
        size_t outputs = 0;
#endif

        AudioEngine() {
            auto config = ma_engine_config_init();
            config.periodSizeInMilliseconds = 5;
#ifdef RACHEL_IOS_AUDIO
            // AVAudioSession is shared with the UIKit video player and managed on the iOS side.
            auto context = ma_context_config_init();
            context.coreaudio.sessionCategory = ma_ios_session_category_none;
            context.coreaudio.noAudioSessionActivate = MA_TRUE;
            context.coreaudio.noAudioSessionDeactivate = MA_TRUE;
            auto playback = ma_device_config_init(ma_device_type_playback);
            playback.playback.format = ma_format_f32;
            playback.periodSizeInMilliseconds = config.periodSizeInMilliseconds;
            playback.pUserData = &value;
            playback.dataCallback = [](ma_device *device, void *output, const void *, ma_uint32 frames) {
                ma_engine_read_pcm_frames(static_cast<ma_engine *>(device->pUserData), output, frames, nullptr);
            };
            check(ma_device_init_ex(nullptr, 0, &context, &playback, &device));
            config.noAutoStart = MA_TRUE;
            config.pDevice = &device;
            try {
                check(ma_engine_init(&config, &value));
            }
            catch (...) {
                ma_device_uninit(&device);
                throw;
            }
#else
            check(ma_engine_init(&config, &value));
#endif
        }

        ~AudioEngine() {
#ifdef RACHEL_IOS_AUDIO
            ma_device_stop(&device);
#endif
            ma_engine_uninit(&value);
#ifdef RACHEL_IOS_AUDIO
            ma_device_uninit(&device);
#endif
        }

        void acquireOutput() {
#ifdef RACHEL_IOS_AUDIO
            std::lock_guard<std::mutex> guard(outputMutex);
            if (outputs == 0) check(ma_engine_start(&value));
            ++outputs;
#endif
        }

        void releaseOutput() {
#ifdef RACHEL_IOS_AUDIO
            std::lock_guard<std::mutex> guard(outputMutex);
            if (--outputs == 0) ma_engine_stop(&value);
#endif
        }
    };

    struct AudioOutput {
        AudioEngine &engine;
        bool active = false;

        void start() {
            if (active) return;
            engine.acquireOutput();
            active = true;
        }

        void stop() {
            if (!active) return;
            active = false;
            engine.releaseOutput();
        }

        ~AudioOutput() { stop(); }
    };

    std::mutex engineMutex;
    std::weak_ptr<AudioEngine> sharedEngine;

    std::shared_ptr<AudioEngine> audioEngine() {
        std::lock_guard<std::mutex> guard(engineMutex);
        auto engine = sharedEngine.lock();
        if (!engine) sharedEngine = engine = std::make_shared<AudioEngine>();
        return engine;
    }

    ma_result loadFile(ma_engine *engine, const char *path, ma_uint32 flags, ma_sound *sound) {
#ifdef _WIN32
        const auto size = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, path, -1, nullptr, 0);
        if (!size) return MA_INVALID_ARGS;
        std::vector<wchar_t> filename(size);
        if (!MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, path, -1, filename.data(), size)) return MA_INVALID_ARGS;
        return ma_sound_init_from_file_w(engine, filename.data(), flags, nullptr, nullptr, sound);
#else
        return ma_sound_init_from_file(engine, path, flags, nullptr, nullptr, sound);
#endif
    }

    int64_t milliseconds(ma_uint64 frames, ma_uint32 rate) {
        if (!rate) return 0;
        const auto seconds = frames / rate;
        if (seconds > static_cast<ma_uint64>(INT64_MAX / 1000)) return INT64_MAX;
        return static_cast<int64_t>(seconds * 1000 + frames % rate * 1000 / rate);
    }

    class MiniaudioBackend final : public RachelAudioBackend {
        std::shared_ptr<AudioEngine> engine;
        AudioOutput output { *engine };
        std::shared_ptr<AudioNotifications> notifications;
        ma_sound sound {};
        ma_uint32 rate = 0;
        ma_uint64 length = 0;
        std::atomic_bool ended {false};
        std::atomic_bool closed {false};
        ma_event completion {};
        std::thread worker;
        std::mutex mutex;

    public:
        MiniaudioBackend(const char *path, std::shared_ptr<AudioNotifications> listener) : engine(audioEngine()), notifications(std::move(listener)) {
            constexpr auto flags = MA_SOUND_FLAG_STREAM | MA_SOUND_FLAG_NO_PITCH | MA_SOUND_FLAG_NO_SPATIALIZATION;
            const auto result = loadFile(&engine->value, path, flags, &sound);
            check(result);
            const auto formatResult = ma_sound_get_data_format(&sound, nullptr, nullptr, &rate, nullptr, 0);
            const auto lengthResult = ma_sound_get_length_in_pcm_frames(&sound, &length);
            if (formatResult != MA_SUCCESS || lengthResult != MA_SUCCESS || !rate) {
                ma_sound_uninit(&sound);
                throw RachelAudioError(formatResult != MA_SUCCESS ? formatResult : lengthResult != MA_SUCCESS ? lengthResult : MA_INVALID_DATA);
            }
            const auto eventResult = ma_event_init(&completion);
            if (eventResult != MA_SUCCESS) {
                ma_sound_uninit(&sound);
                throw RachelAudioError(eventResult);
            }
            try {
                worker = std::thread([this] {
                    for (;;) {
                        ma_event_wait(&completion);
                        if (closed.load()) break;
                        std::lock_guard<std::mutex> guard(mutex);
                        if (ended.load()) {
                            output.stop();
                            notifications->emit(RACHEL_AUDIO_EVENT_ENDED);
                        }
                    }
                });
                check(ma_sound_set_end_callback(&sound, [](void *context, ma_sound *) {
                    auto *owner = static_cast<MiniaudioBackend *>(context);
                    owner->ended.store(true, std::memory_order_release);
                    ma_event_signal(&owner->completion);
                }, this));
            }
            catch (...) {
                closed.store(true);
                ma_event_signal(&completion);
                if (worker.joinable()) worker.join();
                ma_event_uninit(&completion);
                ma_sound_uninit(&sound);
                throw;
            }
            notifications->emit(RACHEL_AUDIO_EVENT_DURATION, milliseconds(length, rate));
            notifications->emit(RACHEL_AUDIO_EVENT_SOURCE);
        }

        ~MiniaudioBackend() override {
            closed.store(true);
            ma_sound_uninit(&sound);
            ma_event_signal(&completion);
            worker.join();
            ma_event_uninit(&completion);
        }

        RachelAudioSnapshot query() override {
            std::lock_guard<std::mutex> guard(mutex);
            ma_uint64 cursor = 0;
            check(ma_sound_get_cursor_in_pcm_frames(&sound, &cursor));
            const bool finished = ended.load(std::memory_order_acquire) || ma_sound_at_end(&sound);
            return {!finished && ma_sound_is_playing(&sound), milliseconds(finished && length ? length : cursor, rate), milliseconds(length, rate), 0};
        }

        void play() override {
            std::lock_guard<std::mutex> guard(mutex);
            if (ended.load() || ma_sound_at_end(&sound)) check(ma_sound_seek_to_pcm_frame(&sound, 0));
            ended.store(false);
            output.start();
            const auto result = ma_sound_start(&sound);
            if (result != MA_SUCCESS) output.stop();
            check(result);
            notifications->emit(RACHEL_AUDIO_EVENT_STATE, 1);
        }

        void pause() override {
            std::lock_guard<std::mutex> guard(mutex);
            check(ma_sound_stop(&sound));
            output.stop();
            notifications->emit(RACHEL_AUDIO_EVENT_STATE, 0);
        }

        void seek(int64_t position) override {
            std::lock_guard<std::mutex> guard(mutex);
            const auto value = static_cast<ma_uint64>(std::max<int64_t>(position, 0));
            const auto seconds = value / 1000;
            const auto maximum = std::numeric_limits<ma_uint64>::max();
            const auto remainder = value % 1000 * rate / 1000;
            auto frame = seconds <= (maximum - remainder) / rate ? seconds * rate + remainder : maximum;
            if (length) frame = std::min(frame, length);
            check(ma_sound_seek_to_pcm_frame(&sound, frame));
            ended.store(false);
        }
    };

    struct SoundEffect {
        ma_sound sound {};
        ma_audio_buffer buffer {};
        void *samples = nullptr;
        bool initialized = false;
        bool buffered = false;

        ~SoundEffect() {
            if (initialized) ma_sound_uninit(&sound);
            if (buffered) ma_audio_buffer_uninit(&buffer);
            if (samples) ma_free(samples, nullptr);
        }
    };

    struct SoundBank {
        std::shared_ptr<AudioEngine> engine = audioEngine();
        AudioOutput output { *engine };
        std::vector<std::unique_ptr<SoundEffect>> effects;
    };
}

std::shared_ptr<RachelAudioBackend> rachel_audio_open(const char *path, RachelAudioBackend *, const std::shared_ptr<AudioNotifications> &notifications) {
    return std::make_shared<MiniaudioBackend>(path, notifications);
}

extern "C" {
    void rachel_audio_reset_device(void) {
#ifdef RACHEL_IOS_AUDIO
        std::lock_guard<std::mutex> guard(engineMutex);
        sharedEngine.reset();
#endif
    }

    void *rachel_sound_create(void) {
        try { return new SoundBank(); }
        catch (...) { return nullptr; }
    }

    void rachel_sound_release(void *handle) { delete static_cast<SoundBank *>(handle); }

    bool rachel_sound_add_file(void *handle, const char *path) {
        if (!handle || !path || !*path) return false;
        try {
            auto *bank = static_cast<SoundBank *>(handle);
            auto effect = std::make_unique<SoundEffect>();
            check(loadFile(&bank->engine->value, path, MA_SOUND_FLAG_DECODE | MA_SOUND_FLAG_NO_PITCH | MA_SOUND_FLAG_NO_SPATIALIZATION, &effect->sound));
            effect->initialized = true;
            bank->effects.push_back(std::move(effect));
            return true;
        }
        catch (...) { return false; }
    }

    bool rachel_sound_add_memory(void *handle, const void *data, int64_t size) {
        if (!handle || !data || size <= 0 || static_cast<uint64_t>(size) > SIZE_MAX) return false;
        try {
            auto *bank = static_cast<SoundBank *>(handle);
            auto effect = std::make_unique<SoundEffect>();
            auto decoder = ma_decoder_config_init(ma_format_f32, 2, ma_engine_get_sample_rate(&bank->engine->value));
            ma_uint64 frames = 0;
            check(ma_decode_memory(data, static_cast<size_t>(size), &decoder, &frames, &effect->samples));
            auto buffer = ma_audio_buffer_config_init(decoder.format, decoder.channels, frames, effect->samples, nullptr);
            buffer.sampleRate = decoder.sampleRate;
            check(ma_audio_buffer_init(&buffer, &effect->buffer));
            effect->buffered = true;
            check(ma_sound_init_from_data_source(&bank->engine->value, &effect->buffer, MA_SOUND_FLAG_NO_PITCH | MA_SOUND_FLAG_NO_SPATIALIZATION, nullptr, &effect->sound));
            effect->initialized = true;
            bank->effects.push_back(std::move(effect));
            return true;
        }
        catch (...) { return false; }
    }

    bool rachel_sound_play(void *handle, int index) {
        if (!handle || index < 0) return false;
        auto *bank = static_cast<SoundBank *>(handle);
        if (static_cast<size_t>(index) >= bank->effects.size()) return false;
        auto *sound = &bank->effects[index]->sound;
        try {
            bank->output.start();
            return ma_sound_stop(sound) == MA_SUCCESS && ma_sound_seek_to_pcm_frame(sound, 0) == MA_SUCCESS && ma_sound_start(sound) == MA_SUCCESS;
        }
        catch (...) { return false; }
    }

    bool rachel_sound_is_playing(void *handle) {
        if (!handle) return false;
        const auto *bank = static_cast<SoundBank *>(handle);
        return std::any_of(bank->effects.begin(), bank->effects.end(), [](const auto &effect) {
            return ma_sound_is_playing(&effect->sound) && !ma_sound_at_end(&effect->sound);
        });
    }

    void rachel_sound_stop(void *handle) {
        if (!handle) return;
        auto *bank = static_cast<SoundBank *>(handle);
        for (auto &effect : bank->effects) ma_sound_stop(&effect->sound);
        bank->output.stop();
    }
}
