#include "rachel_audio_internal.h"
#include <mutex>
#include <condition_variable>
#include <new>

namespace {
    struct AudioHandle {
        std::mutex mutex;
        std::condition_variable idle;
        std::shared_ptr<RachelAudioBackend> backend;
        std::shared_ptr<AudioCallbacks> callbacks;
        bool music;
        bool video = false;
        size_t readers = 0;
        int64_t revision = 0;
        bool loading = false;
        int64_t error = 0;

        AudioHandle(bool isMusic, std::shared_ptr<AudioCallbacks> listener) : callbacks(std::move(listener)), music(isMusic) { }
        ~AudioHandle() {
            callbacks->close();
            backend.reset();
        }
    };

    struct ReadLease {
        AudioHandle *owner;
        std::shared_ptr<RachelAudioBackend> backend;

        explicit ReadLease(AudioHandle *value) : owner(value) { }

        ~ReadLease() {
            if (!backend) return;
            backend.reset();
            std::lock_guard<std::mutex> guard(owner->mutex);
            if (--owner->readers == 0) owner->idle.notify_all();
        }
    };

    bool beginRead(AudioHandle *owner, int64_t revision, ReadLease &read) {
        if (!owner) return false;
        std::lock_guard<std::mutex> guard(owner->mutex);
        if (owner->revision != revision || owner->loading || owner->error || !owner->backend) return false;
        read.backend = owner->backend;
        ++owner->readers;
        return true;
    }

    template <typename F>
    bool command(void *handle, int64_t revision, F action) noexcept {
        auto *owner = static_cast<AudioHandle *>(handle);
        if (!owner) return false;
        try {
            std::lock_guard<std::mutex> guard(owner->mutex);
            if (!owner->backend || owner->loading || owner->revision != revision) return false;
            try {
                action(*owner->backend);
                owner->error = 0;
                return true;
            }
            catch (const RachelAudioError &failure) {
                owner->error = failure.code;
                if (owner->music) owner->callbacks->emit(revision, RACHEL_AUDIO_EVENT_ERROR, failure.code, failure.what());
            }
            catch (const std::exception &failure) {
                owner->error = -1;
                if (owner->music) owner->callbacks->emit(revision, RACHEL_AUDIO_EVENT_ERROR, -1, failure.what());
            }
            catch (...) {
                owner->error = -1;
                if (owner->music) owner->callbacks->emit(revision, RACHEL_AUDIO_EVENT_ERROR, -1);
            }
        }
        catch (...) { }
        return false;
    }
}

extern "C" {
    void *rachel_audio_create(bool music, RachelAudioCallback callback, RachelAudioCallbackRelease release, void *context) {
        std::shared_ptr<AudioCallbacks> callbacks;
        try {
            callbacks = std::make_shared<AudioCallbacks>(callback, release, context);
            return new AudioHandle(music, callbacks);
        }
        catch (...) {
            if (!callbacks && release) release(context);
            return nullptr;
        }
    }

    void rachel_audio_release(void *handle) {
        delete static_cast<AudioHandle *>(handle);
    }

    void *rachel_video_create(RachelAudioCallback callback, RachelAudioCallbackRelease release, void *context) {
        auto *owner = static_cast<AudioHandle *>(rachel_audio_create(true, callback, release, context));
        if (owner) owner->video = true;
        return owner;
    }

    bool rachel_audio_load(void *handle, const char *path, int64_t revision) {
        auto *owner = static_cast<AudioHandle *>(handle);
        if (!owner || !path || !*path) return false;
        int64_t error = -1;
        std::string message;
        try {
            std::shared_ptr<RachelAudioBackend> previous;
            {
                std::unique_lock<std::mutex> guard(owner->mutex);
                if (revision <= owner->revision) return false;
                owner->revision = revision;
                owner->loading = true;
                owner->error = 0;
                previous.swap(owner->backend);
                if (previous) owner->idle.wait(guard, [owner] { return owner->readers == 0; });
                if (owner->revision != revision) return false;
            }
            if (previous) {
                try { previous->pause(); }
                catch (...) { }
            }
            auto notifications = std::make_shared<AudioNotifications>(owner->callbacks, revision, owner->music);
            std::shared_ptr<RachelAudioBackend> next;
            if (owner->video) next = rachel_video_open(path, notifications);
            else next = rachel_audio_open(path, previous.get(), notifications);
            previous.reset();
            {
                std::lock_guard<std::mutex> guard(owner->mutex);
                if (owner->revision != revision) return false;
                next.swap(owner->backend);
                owner->loading = false;
            }
            return true;
        }
        catch (const RachelAudioError &failure) {
            error = failure.code;
            message = failure.what();
        }
        catch (...) { }
        std::lock_guard<std::mutex> guard(owner->mutex);
        if (owner->revision == revision) {
            owner->error = error;
            owner->loading = false;
            if (owner->music) owner->callbacks->emit(revision, RACHEL_AUDIO_EVENT_ERROR, error, message.empty() ? nullptr : message.c_str());
        }
        return false;
    }

    bool rachel_audio_play(void *handle, int64_t revision) {
        return command(handle, revision, [](RachelAudioBackend &backend) { backend.play(); });
    }

    bool rachel_audio_pause(void *handle, int64_t revision) {
        return command(handle, revision, [](RachelAudioBackend &backend) { backend.pause(); });
    }

    bool rachel_audio_seek(void *handle, int64_t position, int64_t revision) {
        return command(handle, revision, [position](RachelAudioBackend &backend) { backend.seek(position < 0 ? 0 : position); });
    }

    void rachel_audio_stop(void *handle, int64_t revision) {
        auto *owner = static_cast<AudioHandle *>(handle);
        if (!owner) return;
        std::shared_ptr<RachelAudioBackend> previous;
        {
            std::unique_lock<std::mutex> guard(owner->mutex);
            if (revision <= owner->revision) return;
            owner->revision = revision;
            previous.swap(owner->backend);
            owner->loading = false;
            owner->error = 0;
            if (previous) owner->idle.wait(guard, [owner] { return owner->readers == 0; });
        }
    }

    void rachel_audio_query(void *handle, int64_t revision, RachelAudioSnapshot *snapshot) {
        if (!snapshot) return;
        *snapshot = {};
        auto *owner = static_cast<AudioHandle *>(handle);
        if (!owner) return;
        try {
            ReadLease read(owner);
            {
                std::lock_guard<std::mutex> guard(owner->mutex);
                if (owner->revision != revision) return;
                if (owner->error) {
                    snapshot->error = owner->error;
                    return;
                }
                if (owner->loading) {
                    return;
                }
                read.backend = owner->backend;
                if (read.backend) ++owner->readers;
            }
            if (read.backend) *snapshot = read.backend->query();
        }
        catch (const RachelAudioError &failure) {
            snapshot->error = failure.code;
        }
        catch (...) {
            snapshot->error = -1;
        }
    }

    bool rachel_video_info(void *handle, int64_t revision, RachelVideoFrameInfo *info) {
        if (!info) return false;
        *info = {};
        auto *owner = static_cast<AudioHandle *>(handle);
        try {
            ReadLease read(owner);
            return beginRead(owner, revision, read) && read.backend->videoInfo(*info);
        }
        catch (...) { return false; }
    }

    bool rachel_video_copy_frame(void *handle, int64_t revision, void *pixels, int64_t capacity, int width, int height) {
        if (!pixels || width <= 0 || height <= 0 || width > INT32_MAX / 4 || static_cast<int64_t>(width) * height * 4 > capacity) return false;
        auto *owner = static_cast<AudioHandle *>(handle);
        try {
            ReadLease read(owner);
            return beginRead(owner, revision, read) && read.backend->copyVideoFrame(pixels, capacity, width, height);
        }
        catch (...) { return false; }
    }

    void *rachel_video_gpu_frame(void *handle, int64_t revision, void *context, RachelVideoFrameInfo *info) {
        if (!info) return nullptr;
        *info = {};
        auto *owner = static_cast<AudioHandle *>(handle);
        try {
            ReadLease read(owner);
            if (!beginRead(owner, revision, read)) return nullptr;
            auto frame = read.backend->gpuVideoFrame(context);
            if (!frame) return nullptr;
            *info = frame->info;
            return frame.release();
        }
        catch (...) { return nullptr; }
    }

    bool rachel_video_gpu_complete(void *frame, void *renderTarget) {
        if (!frame) return false;
        try { return static_cast<RachelVideoFrame *>(frame)->complete(renderTarget); }
        catch (...) { return false; }
    }

    void rachel_video_gpu_release(void *frame) { delete static_cast<RachelVideoFrame *>(frame); }
}
