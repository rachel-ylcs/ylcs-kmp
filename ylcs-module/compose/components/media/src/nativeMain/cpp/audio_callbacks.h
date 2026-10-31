#pragma once
#include "rachel_audio.h"
#include <memory>
#include <mutex>

class AudioCallbacks {
    std::mutex mutex;
    RachelAudioCallback callback;
    RachelAudioCallbackRelease release;
    void *context;
public:
    AudioCallbacks(RachelAudioCallback listener, RachelAudioCallbackRelease cleanup, void *data) : callback(listener), release(cleanup), context(data) { }
    ~AudioCallbacks() { close(); }

    void emit(int64_t revision, int event, int64_t value = 0, const char *message = nullptr) {
        std::lock_guard<std::mutex> guard(mutex);
        if (callback) {
            const RachelAudioEvent change {revision, event, value, message};
            callback(context, &change);
        }
    }

    void close() {
        void *previous;
        RachelAudioCallbackRelease cleanup;
        {
            std::lock_guard<std::mutex> guard(mutex);
            callback = nullptr;
            cleanup = release;
            previous = context;
            release = nullptr;
            context = nullptr;
        }
        if (cleanup) cleanup(previous);
    }
};

class AudioNotifications {
    std::shared_ptr<AudioCallbacks> callbacks;
    int64_t revision;
public:
    const bool music;

    AudioNotifications(std::shared_ptr<AudioCallbacks> listener, int64_t version, bool isMusic) : callbacks(std::move(listener)), revision(version), music(isMusic) { }

    void emit(int event, int64_t value = 0, const char *message = nullptr) const {
        if (music || event == RACHEL_AUDIO_EVENT_ENDED) callbacks->emit(revision, event, value, message);
    }
};
