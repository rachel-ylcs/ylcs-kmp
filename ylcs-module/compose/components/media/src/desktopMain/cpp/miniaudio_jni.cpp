#include <native_jni.h>
#include <miniaudio.h>

#include <atomic>
#include <cmath>
#include <mutex>
#include <new>
#include <string>

namespace {
    constexpr jint PLAYBACK_STATE_NONE = 0;
    constexpr jint PLAYBACK_STATE_OPENING = 1;
    constexpr jint PLAYBACK_STATE_PLAYING = 3;
    constexpr jint PLAYBACK_STATE_PAUSED = 4;

    enum class PlayerType { Music, Audio };

    jmethodID g_method_nativeMusicDurationChange = nullptr;
    jmethodID g_method_nativeMusicPlaybackStateChange = nullptr;
    jmethodID g_method_nativeMusicSourceChange = nullptr;
    jmethodID g_method_nativeMusicMediaEnded = nullptr;
    jmethodID g_method_nativeMusicOnError = nullptr;
    jmethodID g_method_nativeAudioMediaEnded = nullptr;

    jlong secondsToMilliseconds(float seconds) {
        if (!std::isfinite(seconds) || seconds <= 0.0F) return 0LL;
        return static_cast<jlong>(std::llround(static_cast<double>(seconds) * 1000.0));
    }

    struct NativePlayer {
        ma_engine engine{};
        ma_sound sound{};
        jobject obj;
        PlayerType type;
        std::atomic<jint> state{PLAYBACK_STATE_NONE};
        std::atomic_bool releasing{false};
        std::mutex callbackMutex;
        bool engineInitialized = false;
        bool soundInitialized = false;

        NativePlayer(jobject ref, PlayerType playerType) : obj(ref), type(playerType) {}

        ma_result init() {
            const ma_result result = ma_engine_init(nullptr, &engine);
            engineInitialized = result == MA_SUCCESS;
            return result;
        }

        void notifyPlaybackState(jint value) {
            state.store(value, std::memory_order_release);
            if (type != PlayerType::Music) return;

            std::scoped_lock lock(callbackMutex);
            if (releasing.load(std::memory_order_acquire) || obj == nullptr) return;
            JVM::JniEnvGuard guard;
            if (!guard) return;
            guard->CallVoidMethod(obj, g_method_nativeMusicPlaybackStateChange, value);
            guard.checkException();
        }

        void notifyDuration(jlong duration) {
            if (type != PlayerType::Music) return;

            std::scoped_lock lock(callbackMutex);
            if (releasing.load(std::memory_order_acquire) || obj == nullptr) return;
            JVM::JniEnvGuard guard;
            if (!guard) return;
            guard->CallVoidMethod(obj, g_method_nativeMusicDurationChange, duration);
            guard.checkException();
        }

        void notifySourceChange() {
            if (type != PlayerType::Music) return;

            std::scoped_lock lock(callbackMutex);
            if (releasing.load(std::memory_order_acquire) || obj == nullptr) return;
            JVM::JniEnvGuard guard;
            if (!guard) return;
            guard->CallVoidMethod(obj, g_method_nativeMusicSourceChange);
            guard.checkException();
        }

        void notifyMediaEnded() {
            std::scoped_lock lock(callbackMutex);
            if (releasing.load(std::memory_order_acquire) || obj == nullptr) return;
            JVM::JniEnvGuard guard;
            if (!guard) return;
            const jmethodID method = type == PlayerType::Music
                ? g_method_nativeMusicMediaEnded
                : g_method_nativeAudioMediaEnded;
            guard->CallVoidMethod(obj, method);
            guard.checkException();
        }

        void notifyError(const char* action, ma_result result) {
            if (type != PlayerType::Music) return;

            std::scoped_lock lock(callbackMutex);
            if (releasing.load(std::memory_order_acquire) || obj == nullptr) return;
            JVM::JniEnvGuard guard;
            if (!guard) return;

            std::string message(action);
            message += ": ";
            message += ma_result_description(result);
            jstring javaMessage = s2j(guard.env, message);
            guard->CallVoidMethod(obj, g_method_nativeMusicOnError, javaMessage);
            guard.checkException();
            guard->DeleteLocalRef(javaMessage);
        }

        void unload(bool notify = true) {
            if (soundInitialized) {
                ma_sound_stop(&sound);
                ma_sound_uninit(&sound);
                soundInitialized = false;
            }
            if (notify) notifyPlaybackState(PLAYBACK_STATE_NONE);
            else state.store(PLAYBACK_STATE_NONE, std::memory_order_release);
        }

        bool load(JNIEnv* env, jstring path) {
            unload();
            if (path == nullptr) return true;

            notifyPlaybackState(PLAYBACK_STATE_OPENING);
            const std::string filePath = j2s(env, path);
            const ma_uint32 flags = MA_SOUND_FLAG_NO_PITCH
                | MA_SOUND_FLAG_NO_SPATIALIZATION
                | (type == PlayerType::Music ? MA_SOUND_FLAG_STREAM : 0);
            ma_result result = ma_sound_init_from_file(&engine, filePath.c_str(), flags, nullptr, nullptr, &sound);
            if (result != MA_SUCCESS) {
                notifyPlaybackState(PLAYBACK_STATE_NONE);
                notifyError("Failed to load audio", result);
                return false;
            }
            soundInitialized = true;

            result = ma_sound_set_end_callback(&sound, [](void* userData, ma_sound*) {
                auto* player = static_cast<NativePlayer*>(userData);
                if (player == nullptr || player->releasing.load(std::memory_order_acquire)) return;
                player->notifyPlaybackState(PLAYBACK_STATE_NONE);
                player->notifyMediaEnded();
            }, this);
            if (result != MA_SUCCESS) {
                unload();
                notifyError("Failed to configure audio callback", result);
                return false;
            }

            float duration = 0.0F;
            ma_sound_get_length_in_seconds(&sound, &duration);
            notifyDuration(secondsToMilliseconds(duration));
            notifySourceChange();
            notifyPlaybackState(PLAYBACK_STATE_PAUSED);
            return true;
        }

        void play() {
            if (!soundInitialized) return;
            if (ma_sound_at_end(&sound)) ma_sound_seek_to_pcm_frame(&sound, 0);

            const ma_result result = ma_sound_start(&sound);
            if (result == MA_SUCCESS) notifyPlaybackState(PLAYBACK_STATE_PLAYING);
            else notifyError("Failed to play audio", result);
        }

        void pause() {
            if (!soundInitialized) return;

            const ma_result result = ma_sound_stop(&sound);
            if (result == MA_SUCCESS) notifyPlaybackState(PLAYBACK_STATE_PAUSED);
            else notifyError("Failed to pause audio", result);
        }

        void seek(jlong position) {
            if (!soundInitialized) return;
            const float seconds = static_cast<float>(position < 0LL ? 0LL : position) / 1000.0F;
            const ma_result result = ma_sound_seek_to_second(&sound, seconds);
            if (result != MA_SUCCESS) notifyError("Failed to seek audio", result);
        }

        jlong getPosition() const {
            if (!soundInitialized) return 0LL;
            float position = 0.0F;
            return ma_sound_get_cursor_in_seconds(&sound, &position) == MA_SUCCESS
                ? secondsToMilliseconds(position)
                : 0LL;
        }

        jlong getDuration() const {
            if (!soundInitialized) return 0LL;
            float duration = 0.0F;
            return ma_sound_get_length_in_seconds(&sound, &duration) == MA_SUCCESS
                ? secondsToMilliseconds(duration)
                : 0LL;
        }

        void release(JNIEnv* env) {
            releasing.store(true, std::memory_order_release);
            unload(false);
            if (engineInitialized) {
                ma_engine_uninit(&engine);
                engineInitialized = false;
            }

            std::scoped_lock lock(callbackMutex);
            if (obj != nullptr) {
                env->DeleteGlobalRef(obj);
                obj = nullptr;
            }
        }
    };

    NativePlayer* playerCast(jlong handle) {
        return reinterpret_cast<NativePlayer*>(handle);
    }

    jlong createPlayer(JNIEnv* env, jobject obj, PlayerType type) {
        jobject instance = env->NewGlobalRef(obj);
        if (instance == nullptr) return 0LL;

        auto* player = new(std::nothrow) NativePlayer(instance, type);
        if (player == nullptr) {
            env->DeleteGlobalRef(instance);
            return 0LL;
        }

        const ma_result result = player->init();
        if (result != MA_SUCCESS) {
            player->notifyError("Failed to initialize miniaudio", result);
            player->release(env);
            delete player;
            return 0LL;
        }
        return reinterpret_cast<jlong>(player);
    }

    bool loadMethodIds(JNIEnv* env) {
        jclass musicClass = env->FindClass("love/yinlin/media/MiniaudioMusicController");
        if (musicClass == nullptr) return false;
        g_method_nativeMusicDurationChange = env->GetMethodID(musicClass, "nativeDurationChange", "(J)V");
        g_method_nativeMusicPlaybackStateChange = env->GetMethodID(musicClass, "nativePlaybackStateChange", "(I)V");
        g_method_nativeMusicSourceChange = env->GetMethodID(musicClass, "nativeSourceChange", "()V");
        g_method_nativeMusicMediaEnded = env->GetMethodID(musicClass, "nativeMediaEnded", "()V");
        g_method_nativeMusicOnError = env->GetMethodID(musicClass, "nativeOnError", "(Ljava/lang/String;)V");
        env->DeleteLocalRef(musicClass);

        jclass audioClass = env->FindClass("love/yinlin/media/MiniaudioAudioController");
        if (audioClass == nullptr) return false;
        g_method_nativeAudioMediaEnded = env->GetMethodID(audioClass, "nativeMediaEnded", "()V");
        env->DeleteLocalRef(audioClass);

        return g_method_nativeMusicDurationChange != nullptr
            && g_method_nativeMusicPlaybackStateChange != nullptr
            && g_method_nativeMusicSourceChange != nullptr
            && g_method_nativeMusicMediaEnded != nullptr
            && g_method_nativeMusicOnError != nullptr
            && g_method_nativeAudioMediaEnded != nullptr;
    }
}

extern "C" {
    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
        JVM::vm = vm;
        JVM::JniEnvGuard guard;
        if (!guard || !loadMethodIds(guard.env)) return JNI_ERR;
        return JNI_VERSION_1_6;
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_media_MiniaudioMusicController_nativeCreate(JNIEnv* env, jobject obj) {
        return createPlayer(env, obj, PlayerType::Music);
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_MiniaudioMusicController_nativeRelease(JNIEnv* env, jobject, jlong handle) {
        if (handle == 0LL) return;
        auto* player = playerCast(handle);
        player->release(env);
        delete player;
    }

    JNIEXPORT jint JNICALL Java_love_yinlin_media_MiniaudioMusicController_nativeGetPlaybackState(JNIEnv*, jobject, jlong handle) {
        return handle == 0LL ? PLAYBACK_STATE_NONE : playerCast(handle)->state.load(std::memory_order_acquire);
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_media_MiniaudioMusicController_nativeGetPosition(JNIEnv*, jobject, jlong handle) {
        return handle == 0LL ? 0LL : playerCast(handle)->getPosition();
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_media_MiniaudioMusicController_nativeSetSource(JNIEnv* env, jobject, jlong handle, jstring path) {
        return handle != 0LL && playerCast(handle)->load(env, path) ? JNI_TRUE : JNI_FALSE;
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_MiniaudioMusicController_nativePlay(JNIEnv*, jobject, jlong handle) {
        if (handle != 0LL) playerCast(handle)->play();
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_MiniaudioMusicController_nativePause(JNIEnv*, jobject, jlong handle) {
        if (handle != 0LL) playerCast(handle)->pause();
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_MiniaudioMusicController_nativeSeek(JNIEnv*, jobject, jlong handle, jlong position) {
        if (handle != 0LL) playerCast(handle)->seek(position);
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_media_MiniaudioAudioController_nativeCreate(JNIEnv* env, jobject obj) {
        return createPlayer(env, obj, PlayerType::Audio);
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_MiniaudioAudioController_nativeRelease(JNIEnv* env, jobject, jlong handle) {
        if (handle == 0LL) return;
        auto* player = playerCast(handle);
        player->release(env);
        delete player;
    }

    JNIEXPORT jint JNICALL Java_love_yinlin_media_MiniaudioAudioController_nativeGetPlaybackState(JNIEnv*, jobject, jlong handle) {
        return handle == 0LL ? PLAYBACK_STATE_NONE : playerCast(handle)->state.load(std::memory_order_acquire);
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_media_MiniaudioAudioController_nativeGetPosition(JNIEnv*, jobject, jlong handle) {
        return handle == 0LL ? 0LL : playerCast(handle)->getPosition();
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_media_MiniaudioAudioController_nativeGetDuration(JNIEnv*, jobject, jlong handle) {
        return handle == 0LL ? 0LL : playerCast(handle)->getDuration();
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_media_MiniaudioAudioController_nativeSetSource(JNIEnv* env, jobject, jlong handle, jstring path) {
        return handle != 0LL && playerCast(handle)->load(env, path) ? JNI_TRUE : JNI_FALSE;
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_MiniaudioAudioController_nativePlay(JNIEnv*, jobject, jlong handle) {
        if (handle != 0LL) playerCast(handle)->play();
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_MiniaudioAudioController_nativePause(JNIEnv*, jobject, jlong handle) {
        if (handle != 0LL) playerCast(handle)->pause();
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_MiniaudioAudioController_nativeSeek(JNIEnv*, jobject, jlong handle, jlong position) {
        if (handle != 0LL) playerCast(handle)->seek(position);
    }
}
