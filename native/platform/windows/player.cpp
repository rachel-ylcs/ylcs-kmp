#include "../../ylcs_jni.h"

#include <winrt/windows.foundation.h>
#include <winrt/windows.storage.h>
#include <winrt/windows.media.core.h>
#include <winrt/windows.media.playback.h>
#pragma comment(lib, "RuntimeObject.lib")

using namespace winrt::Windows::Storage;
using namespace winrt::Windows::Media;

struct NativePlayer {
	Playback::MediaPlayer player;
};

static inline Playback::MediaPlayer& np_cast(jlong handle) { return reinterpret_cast<NativePlayer*>(handle)->player; }

extern "C" {
	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_NativeMusicPlayer_nativeCreatePlayer(JNIEnv* env, jobject) {
		auto nativePlayer = new NativePlayer;
		nativePlayer->player.MediaEnded([](Playback::MediaPlayer const& sender, auto&& args) {
			sender.Source(nullptr);
		});
		return reinterpret_cast<jlong>(nativePlayer);
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeMusicPlayer_nativeReleasePlayer(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return;
		auto nativePlayer = reinterpret_cast<NativePlayer*>(handle);
		nativePlayer->player.Close();
		delete nativePlayer;
	}

	JNIEXPORT jboolean JNICALL Java_love_yinlin_platform_NativeMusicPlayer_nativeIsPlaying(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return JNI_FALSE;
		return np_cast(handle).PlaybackSession().PlaybackState() == Playback::MediaPlaybackState::Playing ? JNI_TRUE : JNI_FALSE;
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_NativeMusicPlayer_nativeGetPosition(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return 0LL;
		return np_cast(handle).PlaybackSession().Position().count() / 10000;
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_NativeMusicPlayer_nativeGetDuration(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return 0LL;
		return np_cast(handle).PlaybackSession().NaturalDuration().count() / 10000;
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeMusicPlayer_nativeLoad(JNIEnv* env, jobject, jlong handle, jstring path) {
		if (handle == 0LL) return;
		try {
			auto str = j2w(env, path);
			std::ranges::replace(str, L'/', L'\\');
			auto file = StorageFile::GetFileFromPathAsync(str).get();
			np_cast(handle).Source(Core::MediaSource::CreateFromStorageFile(file));
		}
		catch (...) { }
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeMusicPlayer_nativePlay(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return;
		auto& player = np_cast(handle);
		if (player.PlaybackSession().PlaybackState() != Playback::MediaPlaybackState::Playing) player.Play();
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeMusicPlayer_nativePause(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return;
		auto& player = np_cast(handle);
		if (player.PlaybackSession().PlaybackState() == Playback::MediaPlaybackState::Playing) player.Pause();
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeMusicPlayer_nativeStop(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return;
		auto& player = np_cast(handle);
		np_cast(handle).Source(nullptr);
	}
}