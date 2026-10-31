#include "macos_video.h"

@interface RachelVideoObservation : NSObject
@property(nonatomic, copy) void (^changed)(NSString *key);
@end

@implementation RachelVideoObservation
- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context {
    if (self.changed) self.changed(keyPath);
}
@end

void attachMacosVideo(const std::shared_ptr<MacosVideoState> &state) {
    RachelVideoObservation *observer = [RachelVideoObservation new];
    std::weak_ptr<MacosVideoState> weak = state;
    observer.changed = ^(NSString *) {
        macosVideoMain(^{
            @autoreleasepool {
                auto current = weak.lock();
                if (!current || current->closed.load()) return;
                @try {
                    AVPlayerItem *item = current->player.currentItem;
                    if (item.status == AVPlayerItemStatusFailed || current->player.status == AVPlayerStatusFailed) {
                        NSError *failure = item.error ?: current->player.error;
                        macosVideoFailure(current, failure.code, failure.localizedDescription.UTF8String);
                        return;
                    }
                    if (item.status == AVPlayerItemStatusReadyToPlay) {
                        macosVideoApplySeek(current);
                        captureMacosVideo(current);
                        current->notifications->emit(RACHEL_AUDIO_EVENT_DURATION, macosVideoMilliseconds(item.duration));
                        if (!current->sourceSent) {
                            current->sourceSent = true;
                            current->notifications->emit(RACHEL_AUDIO_EVENT_SOURCE);
                        }
                    }
                    current->notifications->emit(RACHEL_AUDIO_EVENT_STATE, static_cast<int64_t>(current->player.timeControlStatus));
                }
                @catch (NSException *exception) {
                    macosVideoFailure(current, -1, exception.reason.UTF8String);
                }
            }
        });
    };
    state->observer = observer;
    const auto options = NSKeyValueObservingOptionInitial | NSKeyValueObservingOptionNew;
    if (state->notifications->music) {
        [state->player addObserver:observer forKeyPath:@"timeControlStatus" options:options context:nullptr];
        state->observingPlayback = true;
    }
    [state->player.currentItem addObserver:observer forKeyPath:@"status" options:options context:nullptr];
    state->observingStatus = true;
    if (state->notifications->music) {
        [state->player.currentItem addObserver:observer forKeyPath:@"duration" options:options context:nullptr];
        state->observingDuration = true;
    }
}

void detachMacosVideo(const std::shared_ptr<MacosVideoState> &state) {
    if (state->observingPlayback) [state->player removeObserver:state->observer forKeyPath:@"timeControlStatus"];
    if (state->observingStatus) [state->player.currentItem removeObserver:state->observer forKeyPath:@"status"];
    if (state->observingDuration) [state->player.currentItem removeObserver:state->observer forKeyPath:@"duration"];
    state->observingPlayback = false;
    state->observingStatus = false;
    state->observingDuration = false;
    state->observer = nil;
}
