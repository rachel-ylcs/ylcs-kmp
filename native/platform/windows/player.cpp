#include "../../ylcs_jni.h"

#include <winrt/windows.foundation.h>
#include <winrt/windows.storage.h>
#include <winrt/windows.media.core.h>
#include <winrt/windows.media.playback.h>
#include <winrt/windows.graphics.imaging.h>
#include <winrt/Windows.Storage.Streams.h>

#include <d3d11.h>
#include <Windows.Graphics.DirectX.Direct3D11.interop.h>
#include <winrt/windows.graphics.directx.direct3d11.h>

#pragma comment(lib, "RuntimeObject.lib")
#pragma comment(lib, "d3d11.lib")

using namespace winrt::Windows::Foundation;
using namespace winrt::Windows::Storage;
using namespace winrt::Windows::Media;
using namespace winrt::Windows::Graphics::Imaging;
using namespace winrt::Windows::Storage::Streams;
using namespace winrt::Windows::Graphics::DirectX::Direct3D11;

inline jlong ms_cast(TimeSpan value) { return value.count() / 10000LL; }
inline TimeSpan ms_cast(jlong value) { return TimeSpan(value * 10000LL); }

extern "C" {
	struct NativePlayer {
		Playback::MediaPlayer player;
		jobject obj;
		NativePlayer(jobject ref) : obj(ref) {}

		void release(JNIEnv* env) {
			player.Close();
			env->DeleteGlobalRef(obj);
			obj = nullptr;
		}

		jint getPlaybackState() { return static_cast<jint>(player.PlaybackSession().PlaybackState()); }

		jlong getPosition() { return static_cast<jlong>(ms_cast(player.PlaybackSession().Position())); }

		jlong getDuration() { return static_cast<jlong>(ms_cast(player.PlaybackSession().NaturalDuration())); }

		void loadFile(JNIEnv* env, jstring path) {
			try {
				if (path) {
					auto str = j2w(env, path);
					std::ranges::replace(str, L'/', L'\\');
					player.Source(Core::MediaSource::CreateFromUri(Uri(str)));
				}
				else player.Source(nullptr);
			}
			catch (...) {
				player.Source(nullptr);
			}
		}

		void play() { player.Play(); }
		void pause() { if (player.CanPause()) player.Pause(); }
		void seek(jlong position) { player.PlaybackSession().Position(ms_cast(position)); }
	};

	static jmethodID g_method_nativeAudioDurationChange = nullptr;
	static jmethodID g_method_nativeAudioPlaybackStateChange = nullptr;
	static jmethodID g_method_nativeAudioSourceChange = nullptr;
	static jmethodID g_method_nativeAudioMediaEnded = nullptr;
	static jmethodID g_method_nativeAudioOnError = nullptr;

	inline NativePlayer* mp_cast(jlong handle) { return reinterpret_cast<NativePlayer*>(handle); }

	void JNICALL Initialize_AudioPlayer(JavaVM* vm, JNIEnv* env) {
		jclass clz = env->FindClass("love/yinlin/platform/WindowsNativeAudioPlayer");
		g_method_nativeAudioDurationChange = env->GetMethodID(clz, "nativeDurationChange", "(J)V");
		g_method_nativeAudioPlaybackStateChange = env->GetMethodID(clz, "nativePlaybackStateChange", "(I)V");
		g_method_nativeAudioSourceChange = env->GetMethodID(clz, "nativeSourceChange", "()V");
		g_method_nativeAudioMediaEnded = env->GetMethodID(clz, "nativeMediaEnded", "()V");
		g_method_nativeAudioOnError = env->GetMethodID(clz, "nativeOnError", "(Ljava/lang/String;)V");
		env->DeleteLocalRef(clz);
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_WindowsNativeAudioPlayer_nativeCreate(JNIEnv* env, jobject obj) {
		auto instance = env->NewGlobalRef(obj);
		auto nativePlayer = new NativePlayer(instance);
		auto& player = nativePlayer->player;

		player.AutoPlay(false);
		player.AudioCategory(Playback::MediaPlayerAudioCategory::Media);

		auto session = player.PlaybackSession();

		session.NaturalDurationChanged([instance](Playback::MediaPlaybackSession const& sender, auto&& args) {
			JVM::JniEnvGuard guard;
			auto duration = ms_cast(sender.NaturalDuration());
			guard->CallVoidMethod(instance, g_method_nativeAudioDurationChange, duration);
			guard.checkException();
		});

		player.CurrentStateChanged([instance](Playback::MediaPlayer const& sender, auto&& args) {
			JVM::JniEnvGuard guard;
			auto state = static_cast<jint>(sender.PlaybackSession().PlaybackState());
			guard->CallVoidMethod(instance, g_method_nativeAudioPlaybackStateChange, state);
			guard.checkException();
		});

		player.SourceChanged([instance](Playback::MediaPlayer const& sender, auto&& args) {
			JVM::JniEnvGuard guard;
			guard->CallVoidMethod(instance, g_method_nativeAudioSourceChange);
			guard.checkException();
		});

		player.MediaEnded([instance](Playback::MediaPlayer const& sender, auto&& args) {
			JVM::JniEnvGuard guard;
			guard->CallVoidMethod(instance, g_method_nativeAudioMediaEnded);
			guard.checkException();
		});

		player.MediaFailed([instance](Playback::MediaPlayer const& sender, Playback::MediaPlayerFailedEventArgs const& args) {
			JVM::JniEnvGuard guard;
			auto message = s2j(guard.env, winrt::to_string(args.ErrorMessage()));
			guard->CallVoidMethod(instance, g_method_nativeAudioOnError, message);
			guard.checkException();
			guard->DeleteLocalRef(message);
		});

		return reinterpret_cast<jlong>(nativePlayer);
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_WindowsNativeAudioPlayer_nativeRelease(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return;
		auto nativePlayer = mp_cast(handle);
		nativePlayer->release(env);
		delete nativePlayer;
	}

	JNIEXPORT jint JNICALL Java_love_yinlin_platform_WindowsNativeAudioPlayer_nativeGetPlaybackState(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return JNI_FALSE;
		return mp_cast(handle)->getPlaybackState();
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_WindowsNativeAudioPlayer_nativeGetPosition(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return 0LL;
		return mp_cast(handle)->getPosition();
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_WindowsNativeAudioPlayer_nativeGetDuration(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return 0LL;
		return mp_cast(handle)->getDuration();
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_WindowsNativeAudioPlayer_nativeSetSource(JNIEnv* env, jobject, jlong handle, jstring path) {
		if (handle == 0LL) return;
		mp_cast(handle)->loadFile(env, path);
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_WindowsNativeAudioPlayer_nativePlay(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return;
		mp_cast(handle)->play();
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_WindowsNativeAudioPlayer_nativePause(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return;
		mp_cast(handle)->pause();
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_WindowsNativeAudioPlayer_nativeSeek(JNIEnv* env, jobject, jlong handle, jlong position) {
		if (handle == 0LL) return;
		mp_cast(handle)->seek(position);
	}
}

extern "C" {
	static winrt::com_ptr<ID3D11Device> d3dDevice = nullptr;

	struct NativeVideoPlayer : NativePlayer {
		int surfaceWidth = 0;
		int surfaceHeight = 0;
		IDirect3DSurface d3dSurface;

		jbyteArray buffer = nullptr;

		void createBuffer(JVM::JniEnvGuard& guard, int bufferSize) {
			jbyteArray result = guard->NewByteArray(bufferSize);
			buffer = static_cast<jbyteArray>(guard->NewGlobalRef(result));
			guard->DeleteLocalRef(result);
		}

		void deleteBuffer(JVM::JniEnvGuard& guard) {
			if (buffer) {
				guard->DeleteGlobalRef(buffer);
				buffer = nullptr;
			}
		}

		void release(JNIEnv* env) {
			NativePlayer::release(env);
			d3dSurface = nullptr;
			if (buffer) {
				env->DeleteGlobalRef(buffer);
				buffer = nullptr;
			}
		}
	};

	static jmethodID g_method_nativeVideoDurationChange = nullptr;
	static jmethodID g_method_nativeVideoPlaybackStateChange = nullptr;
	static jmethodID g_method_nativeVideoSourceChange = nullptr;
	static jmethodID g_method_nativeVideoMediaEnded = nullptr;
	static jmethodID g_method_nativeVideoOnError = nullptr;
	static jmethodID g_method_nativeVideoIsUpdateFrame = nullptr;
	static jmethodID g_method_nativeVideoFrameAvailable = nullptr;

	inline NativeVideoPlayer* vp_cast(jlong handle) { return reinterpret_cast<NativeVideoPlayer*>(handle); }

	void JNICALL Initialize_VideoPlayer(JavaVM* vm, JNIEnv* env) {
		jclass clz = env->FindClass("love/yinlin/platform/WindowsNativeVideoPlayer");
		g_method_nativeVideoDurationChange = env->GetMethodID(clz, "nativeDurationChange", "(J)V");
		g_method_nativeVideoPlaybackStateChange = env->GetMethodID(clz, "nativePlaybackStateChange", "(I)V");
		g_method_nativeVideoSourceChange = env->GetMethodID(clz, "nativeSourceChange", "()V");
		g_method_nativeVideoMediaEnded = env->GetMethodID(clz, "nativeMediaEnded", "()V");
		g_method_nativeVideoOnError = env->GetMethodID(clz, "nativeOnError", "(Ljava/lang/String;)V");
		g_method_nativeVideoFrameAvailable = env->GetMethodID(clz, "nativeVideoFrameAvailable", "(III[B)V");
		g_method_nativeVideoIsUpdateFrame = env->GetMethodID(clz, "isUpdateFrame", "()Z");
		env->DeleteLocalRef(clz);
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_WindowsNativeVideoPlayer_nativeCreate(JNIEnv* env, jobject obj) {
		auto instance = env->NewGlobalRef(obj);
		auto nativePlayer = new NativeVideoPlayer(instance);
		auto& player = nativePlayer->player;

		player.AutoPlay(false);
		player.AudioCategory(Playback::MediaPlayerAudioCategory::Movie);
		player.IsVideoFrameServerEnabled(true);

		auto session = player.PlaybackSession();

		session.NaturalDurationChanged([instance](Playback::MediaPlaybackSession const& sender, auto&& args) {
			JVM::JniEnvGuard guard;
			auto duration = ms_cast(sender.NaturalDuration());
			guard->CallVoidMethod(instance, g_method_nativeVideoDurationChange, duration);
			guard.checkException();
		});

		player.CurrentStateChanged([instance](Playback::MediaPlayer const& sender, auto&& args) {
			JVM::JniEnvGuard guard;
			auto state = static_cast<jint>(sender.PlaybackSession().PlaybackState());
			guard->CallVoidMethod(instance, g_method_nativeVideoPlaybackStateChange, state);
			guard.checkException();
		});

		player.SourceChanged([instance](Playback::MediaPlayer const& sender, auto&& args) {
			JVM::JniEnvGuard guard;
			guard->CallVoidMethod(instance, g_method_nativeVideoSourceChange);
			guard.checkException();
		});

		player.MediaEnded([instance](Playback::MediaPlayer const& sender, auto&& args) {
			JVM::JniEnvGuard guard;
			guard->CallVoidMethod(instance, g_method_nativeVideoMediaEnded);
			guard.checkException();
		});

		player.MediaFailed([instance](Playback::MediaPlayer const& sender, Playback::MediaPlayerFailedEventArgs const& args) {
			JVM::JniEnvGuard guard;
			auto message = s2j(guard.env, winrt::to_string(args.ErrorMessage()));
			guard->CallVoidMethod(instance, g_method_nativeVideoOnError, message);
			guard.checkException();
			guard->DeleteLocalRef(message);
		});

		player.VideoFrameAvailable([nativePlayer](Playback::MediaPlayer const& sender, auto&& args) {
			JVM::JniEnvGuard guard;
			
			if (sender.PlaybackSession().PlaybackState() != Playback::MediaPlaybackState::Playing) return;
			if (guard->CallBooleanMethod(nativePlayer->obj, g_method_nativeVideoIsUpdateFrame)) return;

			if (!d3dDevice) {
				D3D11CreateDevice(
					nullptr, D3D_DRIVER_TYPE_HARDWARE,
					nullptr, D3D11_CREATE_DEVICE_BGRA_SUPPORT,
					nullptr, 0, D3D11_SDK_VERSION,
					d3dDevice.put(), nullptr, nullptr
				);
				nativePlayer->surfaceWidth = nativePlayer->surfaceHeight = 0;
				nativePlayer->deleteBuffer(guard);
			}
			auto videoWidth = sender.PlaybackSession().NaturalVideoWidth();
			auto videoHeight = sender.PlaybackSession().NaturalVideoHeight();
			if (videoWidth != nativePlayer->surfaceWidth || videoHeight != nativePlayer->surfaceHeight) {
				D3D11_TEXTURE2D_DESC desc{
					.Width = videoWidth,
					.Height = videoHeight,
					.MipLevels = 1,
					.ArraySize = 1,
					.Format = DXGI_FORMAT_B8G8R8A8_UNORM,
					.SampleDesc = { 1, 0 },
					.Usage = {},
					.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET,
					.CPUAccessFlags = 0,
					.MiscFlags = 0
				};
				winrt::com_ptr<ID3D11Texture2D> texture;
				d3dDevice->CreateTexture2D(&desc, nullptr, texture.put());
				auto dxgiSurface = texture.as<IDXGISurface>();
				winrt::com_ptr<::IInspectable> graphics_surface;
				CreateDirect3D11SurfaceFromDXGISurface(dxgiSurface.get(), graphics_surface.put());

				nativePlayer->d3dSurface = graphics_surface.as<IDirect3DSurface>();
				nativePlayer->surfaceWidth = videoWidth;
				nativePlayer->surfaceHeight = videoHeight;
				nativePlayer->deleteBuffer(guard);
			}

			if (nativePlayer->d3dSurface) {
				sender.CopyFrameToVideoSurface(nativePlayer->d3dSurface);
				SoftwareBitmap bitmap = SoftwareBitmap::CreateCopyFromSurfaceAsync(nativePlayer->d3dSurface).get();
				auto buffer = bitmap.LockBuffer(BitmapBufferAccessMode::Read);
				auto reference = buffer.CreateReference();
				int frameWidth = bitmap.PixelWidth();
				int frameHeight = bitmap.PixelHeight();
				int bufferSize = reference.Capacity();

				if (!nativePlayer->buffer) nativePlayer->createBuffer(guard, bufferSize);
				if (nativePlayer->buffer && frameWidth > 0 && frameHeight > 0 && bufferSize > 0) {
					guard->SetByteArrayRegion(nativePlayer->buffer, 0, bufferSize, reinterpret_cast<const jbyte*>(reference.data()));
					guard->CallVoidMethod(nativePlayer->obj, g_method_nativeVideoFrameAvailable, frameWidth, frameHeight, bufferSize, nativePlayer->buffer);
					guard.checkException();
				}

				reference.Close();
				buffer.Close();
				bitmap.Close();
			}
		});

		return reinterpret_cast<jlong>(nativePlayer);
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_WindowsNativeVideoPlayer_nativeRelease(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return;
		auto nativePlayer = vp_cast(handle);
		nativePlayer->release(env);
		delete nativePlayer;
	}

	JNIEXPORT jint JNICALL Java_love_yinlin_platform_WindowsNativeVideoPlayer_nativeGetPlaybackState(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return JNI_FALSE;
		return vp_cast(handle)->getPlaybackState();
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_WindowsNativeVideoPlayer_nativeGetPosition(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return 0LL;
		return vp_cast(handle)->getPosition();
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_WindowsNativeVideoPlayer_nativeGetDuration(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return 0LL;
		return vp_cast(handle)->getDuration();
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_WindowsNativeVideoPlayer_nativeSetSource(JNIEnv* env, jobject, jlong handle, jstring path) {
		if (handle == 0LL) return;
		vp_cast(handle)->loadFile(env, path);
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_WindowsNativeVideoPlayer_nativePlay(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return;
		vp_cast(handle)->play();
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_WindowsNativeVideoPlayer_nativePause(JNIEnv* env, jobject, jlong handle) {
		if (handle == 0LL) return;
		vp_cast(handle)->pause();
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_WindowsNativeVideoPlayer_nativeSeek(JNIEnv* env, jobject, jlong handle, jlong position) {
		if (handle == 0LL) return;
		vp_cast(handle)->seek(position);
	}
}