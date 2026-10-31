#ifndef RACHEL_AUDIO_INTERNAL_H
#define RACHEL_AUDIO_INTERNAL_H

#include "rachel_audio.h"
#include "audio_callbacks.h"
#include <memory>
#include <stdexcept>

class RachelAudioError : public std::runtime_error {
public:
    int64_t code;
    explicit RachelAudioError(int64_t value, const std::string &message = "Audio operation failed") : std::runtime_error(message), code(value ? value : -1) {}
};

class RachelVideoFrame {
public:
    RachelVideoFrameInfo info {};
    virtual ~RachelVideoFrame() = default;
    virtual bool complete(void *renderTarget) = 0;
};

class RachelAudioBackend {
public:
    virtual ~RachelAudioBackend() = default;
    virtual RachelAudioSnapshot query() = 0;
    virtual void play() = 0;
    virtual void pause() = 0;
    virtual void seek(int64_t position) = 0;
    virtual bool videoInfo(RachelVideoFrameInfo &) { return false; }
    virtual bool copyVideoFrame(void *, int64_t, int, int) { return false; }
    virtual std::unique_ptr<RachelVideoFrame> gpuVideoFrame(void *) { return nullptr; }
};

std::shared_ptr<RachelAudioBackend> rachel_audio_open(const char *path, RachelAudioBackend *previous, const std::shared_ptr<AudioNotifications> &notifications);
std::shared_ptr<RachelAudioBackend> rachel_video_open(const char *path, const std::shared_ptr<AudioNotifications> &notifications);

#endif
