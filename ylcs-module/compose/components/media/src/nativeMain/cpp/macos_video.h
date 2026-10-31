#pragma once
#include "rachel_audio_internal.h"
#import <AVFoundation/AVFoundation.h>
#import <CoreMedia/CoreMedia.h>
#import <CoreVideo/CoreVideo.h>
#import <dispatch/dispatch.h>
#include <atomic>
#include <mutex>

struct MacosVideoState {
    AVPlayer *player = nil;
    id endedObserver = nil;
    id failedObserver = nil;
    id frameObserver = nil;
    AVPlayerItemVideoOutput *output = nil;
    std::mutex frames;
    CVPixelBufferRef frame = nullptr;
    int64_t serial = 0;
    std::atomic_bool pendingFrame {false};
    NSObject *observer = nil;
    std::shared_ptr<AudioNotifications> notifications;
    std::atomic_bool closed {false};
    std::atomic<int64_t> error {0};
    bool observingPlayback = false;
    bool observingStatus = false;
    bool observingDuration = false;
    bool sourceSent = false;
    bool wantsPlay = false;
    bool ended = false;
    bool seeking = false;
    bool seekPending = false;
    int64_t seekTarget = 0;
    uint64_t seekVersion = 0;

    ~MacosVideoState() { if (frame) CVPixelBufferRelease(frame); }
};

int64_t macosVideoMilliseconds(CMTime time);
void macosVideoMain(dispatch_block_t action);
void macosVideoFailure(const std::shared_ptr<MacosVideoState> &state, int64_t code, const char *message);
void macosVideoApplySeek(const std::shared_ptr<MacosVideoState> &state);
void attachMacosVideo(const std::shared_ptr<MacosVideoState> &state);
void detachMacosVideo(const std::shared_ptr<MacosVideoState> &state);
void captureMacosVideo(const std::shared_ptr<MacosVideoState> &state);
