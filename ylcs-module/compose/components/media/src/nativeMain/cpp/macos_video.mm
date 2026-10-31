#include "macos_video.h"
#import <AVFoundation/AVFoundation.h>
#import <CoreMedia/CoreMedia.h>
#import <dispatch/dispatch.h>
#include <algorithm>
#include <atomic>
#include <cstring>

void captureMacosVideo(const std::shared_ptr<MacosVideoState> &state) {
    if (state->closed.load() || state->seeking || !state->output) return;
    const auto time = state->player.currentTime;
    if (![state->output hasNewPixelBufferForItemTime:time]) return;
    auto next = [state->output copyPixelBufferForItemTime:time itemTimeForDisplay:nullptr];
    if (!next) return;
    CVPixelBufferRef previous;
    {
        std::lock_guard<std::mutex> guard(state->frames);
        previous = state->frame;
        state->frame = next;
        ++state->serial;
    }
    if (previous) CVPixelBufferRelease(previous);
    if (!state->pendingFrame.exchange(true)) state->notifications->emit(RACHEL_AUDIO_EVENT_FRAME);
}

int64_t macosVideoMilliseconds(CMTime time) {
    if (!CMTIME_IS_NUMERIC(time)) return 0;
    const auto scaled = CMTimeConvertScale(time, 1000, kCMTimeRoundingMethod_RoundTowardZero);
    return std::max<int64_t>(scaled.value, 0);
}

void macosVideoMain(dispatch_block_t action) {
    if ([NSThread isMainThread]) action();
    else dispatch_async(dispatch_get_main_queue(), action);
}

void macosVideoFailure(const std::shared_ptr<MacosVideoState> &state, int64_t code, const char *message) {
    state->error.store(code ? code : -1);
    state->wantsPlay = false;
    state->notifications->emit(RACHEL_AUDIO_EVENT_ERROR, state->error.load(), message);
}

void macosVideoApplySeek(const std::shared_ptr<MacosVideoState> &state) {
    if (!state->seekPending || state->player.currentItem.status != AVPlayerItemStatusReadyToPlay) return;
    state->seekPending = false;
    const auto version = state->seekVersion;
    const auto duration = macosVideoMilliseconds(state->player.currentItem.duration);
    const auto target = duration ? std::min(state->seekTarget, duration) : state->seekTarget;
    state->seekTarget = target;
    std::weak_ptr<MacosVideoState> weak = state;
    [state->player seekToTime:CMTimeMake(target, 1000) toleranceBefore:kCMTimeZero toleranceAfter:kCMTimeZero completionHandler:^(BOOL finished) {
        macosVideoMain(^{
            if (auto current = weak.lock(); current && !current->closed.load() && current->seekVersion == version) {
                current->seeking = false;
                if (!finished) macosVideoFailure(current, -1, "Video seek failed");
                else captureMacosVideo(current);
            }
        });
    }];
}

void requestSeek(const std::shared_ptr<MacosVideoState> &state, int64_t target) {
    ++state->seekVersion;
    state->seekTarget = std::max<int64_t>(target, 0);
    state->seekPending = true;
    state->seeking = true;
    state->ended = false;
    [state->player.currentItem cancelPendingSeeks];
    macosVideoApplySeek(state);
}

class MacosVideoPlayer final : public RachelAudioBackend {
    std::shared_ptr<MacosVideoState> state = std::make_shared<MacosVideoState>();
public:
    MacosVideoPlayer(const char *path, const std::shared_ptr<AudioNotifications> &notifications) {
        state->notifications = notifications;
        const auto current = state;
        @autoreleasepool {
            NSString *text = [NSString stringWithUTF8String:path];
            if (!text) throw RachelAudioError(-1);
            macosVideoMain(^{
                @autoreleasepool {
                    @try {
                        if (current->closed.load()) return;
                        NSURL *url = [text hasPrefix:@"file:"] || [text hasPrefix:@"https:"] || [text hasPrefix:@"http:"]
                            ? [NSURL URLWithString:text] : [NSURL fileURLWithPath:text];
                        if (!url) {
                            macosVideoFailure(current, -1, "Invalid video URL");
                            return;
                        }
                        AVPlayerItem *item = [AVPlayerItem playerItemWithURL:url];
                        current->output = [[AVPlayerItemVideoOutput alloc] initWithPixelBufferAttributes:@{
                            (__bridge NSString *)kCVPixelBufferPixelFormatTypeKey: @(kCVPixelFormatType_32BGRA),
                            (__bridge NSString *)kCVPixelBufferIOSurfacePropertiesKey: @{}
                        }];
                        current->output.suppressesPlayerRendering = YES;
                        [item addOutput:current->output];
                        current->player = [AVPlayer playerWithPlayerItem:item];
                        current->player.actionAtItemEnd = AVPlayerActionAtItemEndPause;
                        attachMacosVideo(current);
                        std::weak_ptr<MacosVideoState> weak = current;
                        current->frameObserver = [current->player addPeriodicTimeObserverForInterval:CMTimeMake(1, 120) queue:dispatch_get_main_queue() usingBlock:^(CMTime) {
                            @autoreleasepool {
                                if (auto owner = weak.lock(); owner && !owner->closed.load()) {
                                    @try { captureMacosVideo(owner); }
                                    @catch (NSException *exception) { macosVideoFailure(owner, -1, exception.reason.UTF8String); }
                                }
                            }
                        }];
                        current->endedObserver = [[NSNotificationCenter defaultCenter] addObserverForName:AVPlayerItemDidPlayToEndTimeNotification object:item queue:[NSOperationQueue mainQueue] usingBlock:^(NSNotification *) {
                            if (auto owner = weak.lock(); owner && !owner->closed.load() && !owner->seeking) {
                                owner->ended = true;
                                owner->wantsPlay = false;
                                owner->notifications->emit(RACHEL_AUDIO_EVENT_ENDED);
                            }
                        }];
                        current->failedObserver = [[NSNotificationCenter defaultCenter] addObserverForName:AVPlayerItemFailedToPlayToEndTimeNotification object:item queue:[NSOperationQueue mainQueue] usingBlock:^(NSNotification *notification) {
                            if (auto owner = weak.lock(); owner && !owner->closed.load()) {
                                NSError *failure = notification.userInfo[AVPlayerItemFailedToPlayToEndTimeErrorKey];
                                macosVideoFailure(owner, failure.code, failure.localizedDescription.UTF8String);
                            }
                        }];
                        if (current->wantsPlay) [current->player play];
                    }
                    @catch (NSException *exception) { macosVideoFailure(current, -1, exception.reason.UTF8String); }
                }
            });
        }
    }

    ~MacosVideoPlayer() override {
        const auto current = state;
        current->closed.store(true);
        macosVideoMain(^{
            @autoreleasepool {
                @try {
                    detachMacosVideo(current);
                    if (current->endedObserver) [[NSNotificationCenter defaultCenter] removeObserver:current->endedObserver];
                    if (current->failedObserver) [[NSNotificationCenter defaultCenter] removeObserver:current->failedObserver];
                    current->endedObserver = nil;
                    current->failedObserver = nil;
                    if (current->frameObserver) [current->player removeTimeObserver:current->frameObserver];
                    current->frameObserver = nil;
                    [current->player pause];
                    [current->player.currentItem cancelPendingSeeks];
                    if (current->output) [current->player.currentItem removeOutput:current->output];
                    current->output = nil;
                    [current->player replaceCurrentItemWithPlayerItem:nil];
                }
                @catch (NSException *exception) { }
                current->player = nil;
            }
        });
    }

    RachelAudioSnapshot query() override {
        const auto current = state;
        __block RachelAudioSnapshot snapshot {};
        dispatch_block_t read = ^{
            @autoreleasepool {
                @try {
                    if (current->closed.load()) return;
                    AVPlayer *player = current->player;
                    AVPlayerItem *item = player.currentItem;
                    NSError *failure = item.error ?: player.error;
                    const auto error = current->error.load();
                    if (error || item.status == AVPlayerItemStatusFailed || player.status == AVPlayerStatusFailed) {
                        snapshot = {false, 0, 0, error ? error : failure.code ? failure.code : -1};
                        return;
                    }
                    macosVideoApplySeek(current);
                    const auto duration = macosVideoMilliseconds(item.duration);
                    auto position = current->seeking ? current->seekTarget : macosVideoMilliseconds(player.currentTime);
                    if (!current->seeking && duration && position >= duration && player.timeControlStatus == AVPlayerTimeControlStatusPaused) current->ended = true;
                    if (current->ended && duration) position = duration;
                    snapshot = {player.timeControlStatus == AVPlayerTimeControlStatusPlaying && !current->ended, position, duration, 0};
                }
                @catch (NSException *exception) { snapshot = {false, 0, 0, -1}; }
            }
        };
        if ([NSThread isMainThread]) read();
        else dispatch_sync(dispatch_get_main_queue(), read);
        return snapshot;
    }

    void play() override {
        const auto current = state;
        macosVideoMain(^{
            @autoreleasepool {
                @try {
                    if (current->closed.load()) return;
                    if (current->ended) requestSeek(current, 0);
                    current->wantsPlay = true;
                    [current->player play];
                }
                @catch (NSException *exception) { macosVideoFailure(current, -1, exception.reason.UTF8String); }
            }
        });
    }

    void pause() override {
        const auto current = state;
        macosVideoMain(^{
            @autoreleasepool {
                @try {
                    if (!current->closed.load()) {
                        current->wantsPlay = false;
                        [current->player pause];
                    }
                }
                @catch (NSException *exception) { macosVideoFailure(current, -1, exception.reason.UTF8String); }
            }
        });
    }

    void seek(int64_t position) override {
        const auto current = state;
        macosVideoMain(^{
            @autoreleasepool {
                @try {
                    if (!current->closed.load()) requestSeek(current, position);
                }
                @catch (NSException *exception) { macosVideoFailure(current, -1, exception.reason.UTF8String); }
            }
        });
    }

    bool videoInfo(RachelVideoFrameInfo &info) override {
        std::lock_guard<std::mutex> guard(state->frames);
        state->pendingFrame.store(false);
        if (!state->frame) return false;
        const auto width = CVPixelBufferGetWidth(state->frame);
        const auto height = CVPixelBufferGetHeight(state->frame);
        if (!width || !height || width > 16384 || height > 16384 || width * height * 4 > INT32_MAX) return false;
        info = {static_cast<int>(width), static_cast<int>(height), state->serial, nullptr};
        return true;
    }

    bool copyVideoFrame(void *pixels, int64_t capacity, int width, int height) override {
        CVPixelBufferRef frame;
        {
            std::lock_guard<std::mutex> guard(state->frames);
            frame = state->frame;
            if (!frame) return false;
            CVPixelBufferRetain(frame);
        }
        const auto rowBytes = static_cast<size_t>(width) * 4;
        const bool valid = CVPixelBufferGetWidth(frame) == static_cast<size_t>(width) && CVPixelBufferGetHeight(frame) == static_cast<size_t>(height) && CVPixelBufferGetPixelFormatType(frame) == kCVPixelFormatType_32BGRA && static_cast<int64_t>(rowBytes) * height <= capacity;
        if (!valid || CVPixelBufferLockBaseAddress(frame, kCVPixelBufferLock_ReadOnly) != kCVReturnSuccess) {
            CVPixelBufferRelease(frame);
            return false;
        }
        const auto stride = CVPixelBufferGetBytesPerRow(frame);
        const auto *source = static_cast<const uint8_t *>(CVPixelBufferGetBaseAddress(frame));
        const bool accessible = source && stride >= rowBytes;
        if (accessible) {
            if (stride == rowBytes) std::memcpy(pixels, source, rowBytes * height);
            else {
                for (int y = 0; y < height; ++y) std::memcpy(static_cast<uint8_t *>(pixels) + y * rowBytes, source + y * stride, rowBytes);
            }
        }
        CVPixelBufferUnlockBaseAddress(frame, kCVPixelBufferLock_ReadOnly);
        CVPixelBufferRelease(frame);
        return accessible;
    }
};

std::shared_ptr<RachelAudioBackend> rachel_video_open(const char *path, const std::shared_ptr<AudioNotifications> &notifications) {
    return std::make_shared<MacosVideoPlayer>(path, notifications);
}
