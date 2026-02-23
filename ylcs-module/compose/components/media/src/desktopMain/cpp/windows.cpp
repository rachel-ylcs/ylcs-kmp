#include <native_jni.h>
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

struct NativePlayer {
    Playback::MediaPlayer player;
    jobject obj;
    NativePlayer(jobject ref) : obj(ref) {}

    void release(JNIEnv* env) {
        pause();
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

struct NativeVideoPlayer : NativePlayer {
    UINT surfaceWidth = 0U;
    UINT surfaceHeight = 0U;

    winrt::com_ptr<ID3D11Device> d3dDevice = nullptr;
    winrt::com_ptr<ID3D11DeviceContext> d3dContext = nullptr;
    IDirect3DSurface d3dSurface = nullptr;
    winrt::com_ptr<ID3D11Texture2D> renderTargetTexture = nullptr;
    winrt::com_ptr<ID3D11Texture2D> stagingTexture = nullptr;

    bool isRendering = false;
    jbyteArray buffer = nullptr;

    winrt::event_token token_duration, token_state, token_opened, token_ended, token_failed, token_frame;

    void setupSurface(JVM::JniEnvGuard& guard, UINT width, UINT height) {
        D3D11_TEXTURE2D_DESC desc{
            .Width = width,
            .Height = height,
            .MipLevels = 1,
            .ArraySize = 1,
            .Format = DXGI_FORMAT_B8G8R8A8_UNORM,
            .SampleDesc = { 1, 0 },
            .Usage = {},
            .BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET,
            .CPUAccessFlags = 0,
            .MiscFlags = 0
        };
        d3dDevice->CreateTexture2D(&desc, nullptr, renderTargetTexture.put());
        D3D11_TEXTURE2D_DESC stagingDesc = desc;
        stagingDesc.Usage = D3D11_USAGE_STAGING;
        stagingDesc.BindFlags = 0;
        stagingDesc.CPUAccessFlags = D3D11_CPU_ACCESS_READ;
        d3dDevice->CreateTexture2D(&stagingDesc, nullptr, stagingTexture.put());
        auto dxgiSurface = renderTargetTexture.as<IDXGISurface>();
        winrt::com_ptr<::IInspectable> graphics_surface;
        CreateDirect3D11SurfaceFromDXGISurface(dxgiSurface.get(), graphics_surface.put());
        d3dSurface = graphics_surface.as<IDirect3DSurface>();

        if (buffer) guard->DeleteGlobalRef(buffer);
        jbyteArray result = guard->NewByteArray(width * height * 4);
        buffer = static_cast<jbyteArray>(guard->NewGlobalRef(result));
        guard->DeleteLocalRef(result);
    }

    void release(JNIEnv* env) {
        auto session = player.PlaybackSession();

        session.NaturalDurationChanged(token_duration);
        player.CurrentStateChanged(token_state);
        player.MediaEnded(token_ended);
        player.MediaFailed(token_failed);
        player.VideoFrameAvailable(token_frame);

        NativePlayer::release(env);
        d3dDevice = nullptr;
        d3dContext = nullptr;
        renderTargetTexture = nullptr;
        stagingTexture = nullptr;
        d3dSurface = nullptr;

        if (buffer) {
            env->DeleteGlobalRef(buffer);
            buffer = nullptr;
        }
    }
};

inline NativePlayer* mp_cast(jlong handle) { return reinterpret_cast<NativePlayer*>(handle); }
inline NativeVideoPlayer* vp_cast(jlong handle) { return reinterpret_cast<NativeVideoPlayer*>(handle); }

extern "C" {
    // Audio Player
    static jmethodID g_method_nativeAudioDurationChange = nullptr;
    static jmethodID g_method_nativeAudioPlaybackStateChange = nullptr;
    static jmethodID g_method_nativeAudioSourceChange = nullptr;
    static jmethodID g_method_nativeAudioMediaEnded = nullptr;
    static jmethodID g_method_nativeAudioOnError = nullptr;

    // Video Player
    static jmethodID g_method_nativeVideoDurationChange = nullptr;
    static jmethodID g_method_nativeVideoPlaybackStateChange = nullptr;
    static jmethodID g_method_nativeVideoMediaEnded = nullptr;
    static jmethodID g_method_nativeVideoOnError = nullptr;
    static jmethodID g_method_nativeVideoFrameSize = nullptr;
    static jmethodID g_method_nativeVideoUpdateFrame = nullptr;

    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
        JVM::vm = vm;
        JVM::JniEnvGuard guard;
        JNIEnv* env = guard.env;

        // Init Audio Player
//        jclass clz1 = env->FindClass("love/yinlin/media/WindowsAudioController");
//        g_method_nativeAudioDurationChange = env->GetMethodID(clz1, "nativeDurationChange", "(J)V");
//        g_method_nativeAudioPlaybackStateChange = env->GetMethodID(clz1, "nativePlaybackStateChange", "(I)V");
//        g_method_nativeAudioSourceChange = env->GetMethodID(clz1, "nativeSourceChange", "()V");
//        g_method_nativeAudioMediaEnded = env->GetMethodID(clz1, "nativeMediaEnded", "()V");
//        g_method_nativeAudioOnError = env->GetMethodID(clz1, "nativeOnError", "(Ljava/lang/String;)V");
//        env->DeleteLocalRef(clz1);

        // Init Video Player
        jclass clz2 = env->FindClass("love/yinlin/media/WindowsVideoController");
        g_method_nativeVideoDurationChange = env->GetMethodID(clz2, "nativeDurationChange", "(J)V");
        g_method_nativeVideoPlaybackStateChange = env->GetMethodID(clz2, "nativePlaybackStateChange", "(I)V");
        g_method_nativeVideoMediaEnded = env->GetMethodID(clz2, "nativeMediaEnded", "()V");
        g_method_nativeVideoOnError = env->GetMethodID(clz2, "nativeOnError", "(Ljava/lang/String;)V");
        g_method_nativeVideoFrameSize = env->GetMethodID(clz2, "nativeFrameSize", "(II)V");
        g_method_nativeVideoUpdateFrame = env->GetMethodID(clz2, "nativeUpdateFrame", "([B)V");
        env->DeleteLocalRef(clz2);

        return JNI_VERSION_1_6;
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_media_WindowsAudioController_nativeCreate(JNIEnv* env, jobject obj) {
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

    JNIEXPORT void JNICALL Java_love_yinlin_media_WindowsAudioController_nativeRelease(JNIEnv* env, jobject, jlong handle) {
        if (handle == 0LL) return;
        auto nativePlayer = mp_cast(handle);
        nativePlayer->release(env);
        delete nativePlayer;
    }

    JNIEXPORT jint JNICALL Java_love_yinlin_media_WindowsAudioController_nativeGetPlaybackState(JNIEnv* env, jobject, jlong handle) {
        if (handle == 0LL) return JNI_FALSE;
        return mp_cast(handle)->getPlaybackState();
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_media_WindowsAudioController_nativeGetPosition(JNIEnv* env, jobject, jlong handle) {
        if (handle == 0LL) return 0LL;
        return mp_cast(handle)->getPosition();
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_media_WindowsAudioController_nativeGetDuration(JNIEnv* env, jobject, jlong handle) {
        if (handle == 0LL) return 0LL;
        return mp_cast(handle)->getDuration();
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_WindowsAudioController_nativeSetSource(JNIEnv* env, jobject, jlong handle, jstring path) {
        if (handle == 0LL) return;
        mp_cast(handle)->loadFile(env, path);
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_WindowsAudioController_nativePlay(JNIEnv* env, jobject, jlong handle) {
        if (handle == 0LL) return;
        mp_cast(handle)->play();
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_WindowsAudioController_nativePause(JNIEnv* env, jobject, jlong handle) {
        if (handle == 0LL) return;
        mp_cast(handle)->pause();
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_WindowsAudioController_nativeSeek(JNIEnv* env, jobject, jlong handle, jlong position) {
        if (handle == 0LL) return;
        mp_cast(handle)->seek(position);
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_media_WindowsVideoController_nativeCreate(JNIEnv* env, jobject obj) {
        auto instance = env->NewGlobalRef(obj);
        auto nativePlayer = new NativeVideoPlayer(instance);
        auto& player = nativePlayer->player;

        player.AutoPlay(false);
        player.AudioCategory(Playback::MediaPlayerAudioCategory::Movie);
        player.IsVideoFrameServerEnabled(true);

        auto session = player.PlaybackSession();

        D3D11CreateDevice(
            nullptr, D3D_DRIVER_TYPE_HARDWARE,
            nullptr, D3D11_CREATE_DEVICE_BGRA_SUPPORT,
            nullptr, 0, D3D11_SDK_VERSION,
            nativePlayer->d3dDevice.put(), nullptr, nativePlayer->d3dContext.put()
        );

        nativePlayer->token_duration = session.NaturalDurationChanged([instance](Playback::MediaPlaybackSession const& sender, auto&& args) {
            JVM::JniEnvGuard guard;
            auto duration = ms_cast(sender.NaturalDuration());
            guard->CallVoidMethod(instance, g_method_nativeVideoDurationChange, duration);
            guard.checkException();
        });

        nativePlayer->token_state = player.CurrentStateChanged([instance](Playback::MediaPlayer const& sender, auto&& args) {
            JVM::JniEnvGuard guard;
            auto state = static_cast<jint>(sender.PlaybackSession().PlaybackState());
            guard->CallVoidMethod(instance, g_method_nativeVideoPlaybackStateChange, state);
            guard.checkException();
        });

        nativePlayer->token_opened = player.MediaOpened([instance, nativePlayer](Playback::MediaPlayer const& sender, auto&& args) {
            JVM::JniEnvGuard guard;

            auto session = sender.PlaybackSession();
            auto width = session.NaturalVideoWidth();
            auto height = session.NaturalVideoHeight();
            nativePlayer->surfaceWidth = width;
            nativePlayer->surfaceHeight = height;
            nativePlayer->setupSurface(guard, width, height);

            guard->CallVoidMethod(instance, g_method_nativeVideoFrameSize, width, height);
            guard.checkException();
        });

        nativePlayer->token_ended = player.MediaEnded([instance](Playback::MediaPlayer const& sender, auto&& args) {
            JVM::JniEnvGuard guard;
            guard->CallVoidMethod(instance, g_method_nativeVideoMediaEnded);
            guard.checkException();
        });

        nativePlayer->token_failed = player.MediaFailed([instance](Playback::MediaPlayer const& sender, Playback::MediaPlayerFailedEventArgs const& args) {
            JVM::JniEnvGuard guard;
            auto message = s2j(guard.env, winrt::to_string(args.ErrorMessage()));
            guard->CallVoidMethod(instance, g_method_nativeVideoOnError, message);
            guard.checkException();
            guard->DeleteLocalRef(message);
        });

        nativePlayer->token_frame = player.VideoFrameAvailable([nativePlayer](Playback::MediaPlayer const& sender, auto&& args) {
            if (nativePlayer->isRendering) return;
            nativePlayer->isRendering = true;

            JVM::JniEnvGuard guard;

            auto width = nativePlayer->surfaceWidth;
            auto height = nativePlayer->surfaceHeight;
            auto buffer = nativePlayer->buffer;
            auto staging = nativePlayer->stagingTexture.get();
            auto target = nativePlayer->renderTargetTexture.get();
            auto d3dContext = nativePlayer->d3dContext;

            if (nativePlayer->d3dSurface && staging && target && buffer) {
                sender.CopyFrameToVideoSurface(nativePlayer->d3dSurface);
                d3dContext->CopyResource(staging, target);

                D3D11_MAPPED_SUBRESOURCE mapped;
                HRESULT hr = d3dContext->Map(staging, 0, D3D11_MAP_READ, 0, &mapped);
                if (SUCCEEDED(hr)) {
                    auto bytesPerRow = width * 4;
                    auto totalBufferSize = bytesPerRow * height;
                    auto src = static_cast<const jbyte*>(mapped.pData);
                    // 对齐直接拷贝
                    if (mapped.RowPitch == bytesPerRow) guard->SetByteArrayRegion(buffer, 0, totalBufferSize, src);
                    else {
                        // 不对齐逐行拷贝
                        auto dest = static_cast<jbyte*>(guard->GetPrimitiveArrayCritical(buffer, nullptr));
                        if (dest) {
                            for (uint32_t y = 0; y < height; ++y) memcpy(dest + (y * bytesPerRow), src + (y * mapped.RowPitch), bytesPerRow);
                            guard->ReleasePrimitiveArrayCritical(buffer, dest, 0);
                        }
                    }

                    guard->CallVoidMethod(nativePlayer->obj, g_method_nativeVideoUpdateFrame, buffer);
                    guard.checkException();

                    d3dContext->Unmap(staging, 0);
                }
            }

            nativePlayer->isRendering = false;
        });

        return reinterpret_cast<jlong>(nativePlayer);
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_WindowsVideoController_nativeRelease(JNIEnv* env, jobject, jlong handle) {
        if (handle == 0LL) return;
        auto nativePlayer = vp_cast(handle);
        nativePlayer->release(env);
        delete nativePlayer;
    }

    JNIEXPORT jint JNICALL Java_love_yinlin_media_WindowsVideoController_nativeGetPlaybackState(JNIEnv* env, jobject, jlong handle) {
        if (handle == 0LL) return JNI_FALSE;
        return vp_cast(handle)->getPlaybackState();
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_media_WindowsVideoController_nativeGetPosition(JNIEnv* env, jobject, jlong handle) {
        if (handle == 0LL) return 0LL;
        return vp_cast(handle)->getPosition();
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_WindowsVideoController_nativeSetSource(JNIEnv* env, jobject, jlong handle, jstring path) {
        if (handle == 0LL) return;
        vp_cast(handle)->loadFile(env, path);
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_WindowsVideoController_nativePlay(JNIEnv* env, jobject, jlong handle) {
        if (handle == 0LL) return;
        vp_cast(handle)->play();
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_WindowsVideoController_nativePause(JNIEnv* env, jobject, jlong handle) {
        if (handle == 0LL) return;
        vp_cast(handle)->pause();
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_WindowsVideoController_nativeSeek(JNIEnv* env, jobject, jlong handle, jlong position) {
        if (handle == 0LL) return;
        vp_cast(handle)->seek(position);
    }
}