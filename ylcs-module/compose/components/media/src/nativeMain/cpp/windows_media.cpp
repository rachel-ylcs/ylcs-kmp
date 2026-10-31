#include "windows_media.h"
#include <shlwapi.h>
#include <algorithm>
#include <limits>
#include <vector>

using winrt::Windows::Foundation::Uri;
using winrt::Windows::Foundation::TimeSpan;
using winrt::Windows::Media::Core::MediaSource;
using winrt::Windows::Media::Playback::MediaPlayer;
using winrt::Windows::Media::Playback::MediaPlaybackState;

WindowsApartment::WindowsApartment() : result(RoInitialize(RO_INIT_MULTITHREADED)) {
    if (result != RPC_E_CHANGED_MODE) winrt::check_hresult(result);
}

WindowsApartment::~WindowsApartment() {
    if (SUCCEEDED(result)) RoUninitialize();
}

WindowsMediaPlayer::WindowsMediaPlayer(const std::shared_ptr<AudioNotifications> &notifications) : state(std::make_shared<WindowsMediaState>()) {
    WindowsApartment apartment;
    winrt::check_hresult(CoIncrementMTAUsage(&apartmentCookie));
    try {
        state->notifications = notifications;
        player = MediaPlayer();
        player.AutoPlay(false);
        player.AudioCategory(winrt::Windows::Media::Playback::MediaPlayerAudioCategory::Media);
        session = player.PlaybackSession();
        std::weak_ptr<WindowsMediaState> weak = state;
        opened = player.MediaOpened(winrt::auto_revoke, [weak](auto const &sender, auto const &) {
            if (auto current = weak.lock(); current && !current->closed.load()) {
                try {
                    applySeek(sender, current);
                    current->notifications->emit(RACHEL_AUDIO_EVENT_DURATION, sender.PlaybackSession().NaturalDuration().count() / 10000);
                    if (current->wantsPlay.load()) sender.Play();
                }
                catch (const winrt::hresult_error &error) {
                    current->error.store(error.code().value);
                    const auto message = winrt::to_string(error.message());
                    current->notifications->emit(RACHEL_AUDIO_EVENT_ERROR, error.code().value, message.c_str());
                }
            }
        });
        ended = player.MediaEnded(winrt::auto_revoke, [weak](auto const &, auto const &) {
            if (auto current = weak.lock(); current && !current->closed.load()) {
                current->ended.store(true);
                current->wantsPlay.store(false);
                current->notifications->emit(RACHEL_AUDIO_EVENT_ENDED);
            }
        });
        failed = player.MediaFailed(winrt::auto_revoke, [weak](auto const &, auto const &args) {
            if (auto current = weak.lock(); current && !current->closed.load()) {
                const auto code = args.ExtendedErrorCode().value;
                current->error.store(code ? code : E_FAIL);
                current->wantsPlay.store(false);
                const auto message = winrt::to_string(args.ErrorMessage());
                current->notifications->emit(RACHEL_AUDIO_EVENT_ERROR, code ? code : E_FAIL, message.c_str());
            }
        });
        playbackState = session.PlaybackStateChanged(winrt::auto_revoke, [weak](auto const &sender, auto const &) {
            if (auto current = weak.lock(); current && !current->closed.load()) current->notifications->emit(RACHEL_AUDIO_EVENT_STATE, static_cast<int64_t>(sender.PlaybackState()));
        });
        duration = session.NaturalDurationChanged(winrt::auto_revoke, [weak](auto const &sender, auto const &) {
            if (auto current = weak.lock(); current && !current->closed.load()) current->notifications->emit(RACHEL_AUDIO_EVENT_DURATION, sender.NaturalDuration().count() / 10000);
        });
        source = player.SourceChanged(winrt::auto_revoke, [weak](auto const &, auto const &) {
            if (auto current = weak.lock(); current && !current->closed.load()) current->notifications->emit(RACHEL_AUDIO_EVENT_SOURCE);
        });
    }
    catch (...) {
        state->closed.store(true);
        opened.revoke();
        ended.revoke();
        failed.revoke();
        playbackState.revoke();
        duration.revoke();
        source.revoke();
        if (player) player.Close();
        session = nullptr;
        player = nullptr;
        CoDecrementMTAUsage(apartmentCookie);
        apartmentCookie = nullptr;
        throw;
    }
}

WindowsMediaPlayer::~WindowsMediaPlayer() {
    state->closed.store(true);
    try {
        WindowsApartment apartment;
        opened.revoke();
        ended.revoke();
        failed.revoke();
        playbackState.revoke();
        duration.revoke();
        source.revoke();
        if (player) player.Close();
        session = nullptr;
        player = nullptr;
    }
    catch (...) { }
    if (apartmentCookie) CoDecrementMTAUsage(apartmentCookie);
}

void WindowsMediaPlayer::load(const char *path) {
    WindowsApartment apartment;
    const auto text = winrt::to_hstring(path);
    if (PathIsURLW(text.c_str())) player.Source(MediaSource::CreateFromUri(Uri(text)));
    else {
        const auto length = GetFullPathNameW(text.c_str(), 0, nullptr, nullptr);
        if (!length) winrt::throw_last_error();
        std::vector<wchar_t> full(length);
        const auto written = GetFullPathNameW(text.c_str(), length, full.data(), nullptr);
        if (!written || written >= length) winrt::throw_last_error();
        if (length > (UINT32_MAX - 32) / 12) throw RachelAudioError(E_INVALIDARG);
        DWORD capacity = length * 12 + 32;
        std::vector<wchar_t> uri(capacity);
        winrt::check_hresult(UrlCreateFromPathW(full.data(), uri.data(), &capacity, 0));
        player.Source(MediaSource::CreateFromUri(Uri(uri.data())));
    }
}

void WindowsMediaPlayer::applySeek(const MediaPlayer &player, const std::shared_ptr<WindowsMediaState> &state) {
    std::lock_guard<std::mutex> guard(state->seekMutex);
    if (!state->seekPending.load()) return;
    const auto session = player.PlaybackSession();
    const auto status = session.PlaybackState();
    if (status == MediaPlaybackState::None || status == MediaPlaybackState::Opening) return;
    const auto duration = session.NaturalDuration().count() / 10000;
    const auto maximum = duration > 0 ? duration : INT64_MAX / 10000;
    const auto target = std::clamp<int64_t>(state->seekTarget.load(), 0, maximum);
    session.Position(TimeSpan(target * 10000));
    state->seekPending.store(false);
    state->ended.store(false);
}

RachelAudioSnapshot WindowsMediaPlayer::query() {
    WindowsApartment apartment;
    std::lock_guard<std::mutex> guard(mutex);
    const auto duration = std::max<int64_t>(session.NaturalDuration().count() / 10000, 0);
    const auto position = state->seekPending.load() ? state->seekTarget.load() : std::max<int64_t>(session.Position().count() / 10000, 0);
    return {session.PlaybackState() == MediaPlaybackState::Playing, state->ended.load() && duration ? duration : position, duration, state->error.load()};
}

void WindowsMediaPlayer::play() {
    WindowsApartment apartment;
    std::lock_guard<std::mutex> guard(mutex);
    state->wantsPlay.store(true);
    if (state->ended.exchange(false)) {
        std::lock_guard<std::mutex> seekGuard(state->seekMutex);
        state->seekTarget.store(0);
        state->seekPending.store(true);
    }
    applySeek(player, state);
    player.Play();
}

void WindowsMediaPlayer::pause() {
    WindowsApartment apartment;
    std::lock_guard<std::mutex> guard(mutex);
    state->wantsPlay.store(false);
    player.Pause();
}

void WindowsMediaPlayer::seek(int64_t position) {
    WindowsApartment apartment;
    std::lock_guard<std::mutex> guard(mutex);
    {
        std::lock_guard<std::mutex> seekGuard(state->seekMutex);
        state->seekTarget.store(std::max<int64_t>(position, 0));
        state->seekPending.store(true);
        state->ended.store(false);
    }
    applySeek(player, state);
}

