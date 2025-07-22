#include "../../ylcs_jni.h"

#include <winrt/windows.foundation.h>
#include <winrt/windows.storage.h>
#include <winrt/windows.media.core.h>
#include <winrt/windows.media.playback.h>

using namespace winrt::Windows::Storage;
using namespace winrt::Windows::Media;

struct NativePlayer {
	Playback::MediaPlayer player;
};

static inline Playback::MediaPlayer& np_cast(jlong handle) { return reinterpret_cast<NativePlayer*>(handle)->player; }

extern "C" {
	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_MusicPlayer_nativeCreatePlayer(JNIEnv* env, jobject) {
		return reinterpret_cast<jlong>(new NativePlayer);
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_MusicPlayer_nativeReleasePlayer(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return;
		auto nativePlayer = reinterpret_cast<NativePlayer*>(handle);
		nativePlayer->player.Close();
		delete nativePlayer;
	}

	JNIEXPORT jboolean JNICALL Java_love_yinlin_platform_MusicPlayer_nativeIsPlaying(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return JNI_FALSE;
		try {
			return np_cast(handle).PlaybackSession().PlaybackState() == Playback::MediaPlaybackState::Playing ? JNI_TRUE : JNI_FALSE;
		}
		catch (...) { return JNI_FALSE; }
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_MusicPlayer_nativeGetPosition(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return 0LL;
		try {
			return np_cast(handle).PlaybackSession().Position().count() / 10000;
		}
		catch (...) { return 0LL; }
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_MusicPlayer_nativeGetDuration(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return 0LL;
		try {
			return np_cast(handle).PlaybackSession().NaturalDuration().count() / 10000;
		}
		catch (...) { return 0LL; }
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_MusicPlayer_nativeLoad(JNIEnv* env, jobject, jlong handle, jstring path) {
		if (handle == 0LL) return;
		try {
			auto str = j2w(env, path);
			std::ranges::replace(str, L'/', L'\\');
			auto file = StorageFile::GetFileFromPathAsync(str).get();
			np_cast(handle).Source(Core::MediaSource::CreateFromStorageFile(file));
		}
		catch (...) { }
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_MusicPlayer_nativePlay(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return;
		try {
			np_cast(handle).Play();
		}
		catch (...) { }
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_MusicPlayer_nativePause(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return;
		try {
			np_cast(handle).Pause();
		}
		catch (...) { }
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_MusicPlayer_nativeStop(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return;
		try {
			np_cast(handle).Source(nullptr);
		}
		catch (...) { }
	}
}