#pragma once
#if defined(__MINGW32__)
#include <coroutine>
#endif
#include <unknwn.h>
#include <roapi.h>
#include <winrt/windows.foundation.h>
#include <winrt/windows.media.core.h>
#include <winrt/windows.media.playback.h>
#include "rachel_audio_internal.h"
#include <atomic>
#include <mutex>

class WindowsApartment {
    HRESULT result;
public:
    WindowsApartment();
    ~WindowsApartment();
};

struct WindowsMediaState {
    std::shared_ptr<AudioNotifications> notifications;
    std::mutex seekMutex;
    std::atomic_bool closed {false};
    std::atomic_bool ended {false};
    std::atomic_bool wantsPlay {false};
    std::atomic_bool seekPending {false};
    std::atomic<int64_t> seekTarget {0};
    std::atomic<int64_t> error {0};
};

class WindowsMediaPlayer : public RachelAudioBackend {
    CO_MTA_USAGE_COOKIE apartmentCookie = nullptr;
    winrt::Windows::Media::Playback::MediaPlayer::MediaOpened_revoker opened;
    winrt::Windows::Media::Playback::MediaPlayer::MediaEnded_revoker ended;
    winrt::Windows::Media::Playback::MediaPlayer::MediaFailed_revoker failed;
    winrt::Windows::Media::Playback::MediaPlaybackSession::PlaybackStateChanged_revoker playbackState;
    winrt::Windows::Media::Playback::MediaPlaybackSession::NaturalDurationChanged_revoker duration;
    winrt::Windows::Media::Playback::MediaPlayer::SourceChanged_revoker source;
protected:
    std::shared_ptr<WindowsMediaState> state;
    winrt::Windows::Media::Playback::MediaPlayer player {nullptr};
    winrt::Windows::Media::Playback::MediaPlaybackSession session {nullptr};
    std::mutex mutex;

    static void applySeek(const winrt::Windows::Media::Playback::MediaPlayer &player, const std::shared_ptr<WindowsMediaState> &state);
public:
    explicit WindowsMediaPlayer(const std::shared_ptr<AudioNotifications> &notifications);
    ~WindowsMediaPlayer() override;
    void load(const char *path);
    RachelAudioSnapshot query() override;
    void play() override;
    void pause() override;
    void seek(int64_t position) override;
};

