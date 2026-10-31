#include "media_jni.h"
#include <string>

extern "C" {
    JNIEXPORT jlong JNICALL Java_love_yinlin_media_AudioNative_createPlayer(JNIEnv *env, jobject, jboolean music, jobject callback) {
        if (!callback) return 0;
        JavaVM *vm = nullptr;
        if (env->GetJavaVM(&vm) != JNI_OK) return 0;
        const auto type = env->FindClass("java/util/function/BiConsumer");
        if (!type) return 0;
        const auto method = env->GetMethodID(type, "accept", "(Ljava/lang/Object;Ljava/lang/Object;)V");
        env->DeleteLocalRef(type);
        if (!method) return 0;
        const auto reference = env->NewGlobalRef(callback);
        if (!reference) return 0;
        JvmAudioCallbacks *listener;
        try { listener = new JvmAudioCallbacks(vm, reference, method); }
        catch (...) {
            env->DeleteGlobalRef(reference);
            return 0;
        }
        return reinterpret_cast<jlong>(rachel_audio_create(music == JNI_TRUE, JvmAudioCallbacks::notify, JvmAudioCallbacks::destroy, listener));
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_AudioNative_release(JNIEnv *, jobject, jlong handle) {
        rachel_audio_release(reinterpret_cast<void *>(handle));
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_media_AudioNative_loadSource(JNIEnv *env, jobject, jlong handle, jbyteArray path, jlong revision) {
        if (!path) return JNI_FALSE;
        try {
            const auto size = env->GetArrayLength(path);
            std::string text(static_cast<size_t>(size), '\0');
            env->GetByteArrayRegion(path, 0, size, reinterpret_cast<jbyte *>(text.data()));
            if (env->ExceptionCheck()) return JNI_FALSE;
            return rachel_audio_load(reinterpret_cast<void *>(handle), text.c_str(), revision) ? JNI_TRUE : JNI_FALSE;
        }
        catch (...) { return JNI_FALSE; }
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_media_AudioNative_play(JNIEnv *, jobject, jlong handle, jlong revision) {
        return rachel_audio_play(reinterpret_cast<void *>(handle), revision) ? JNI_TRUE : JNI_FALSE;
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_media_AudioNative_pause(JNIEnv *, jobject, jlong handle, jlong revision) {
        return rachel_audio_pause(reinterpret_cast<void *>(handle), revision) ? JNI_TRUE : JNI_FALSE;
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_media_AudioNative_seek(JNIEnv *, jobject, jlong handle, jlong position, jlong revision) {
        return rachel_audio_seek(reinterpret_cast<void *>(handle), position, revision) ? JNI_TRUE : JNI_FALSE;
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_AudioNative_stop(JNIEnv *, jobject, jlong handle, jlong revision) {
        rachel_audio_stop(reinterpret_cast<void *>(handle), revision);
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_AudioNative_read(JNIEnv *env, jobject, jlong handle, jlong revision, jlongArray output) {
        if (!output || env->GetArrayLength(output) < 4) return;
        RachelAudioSnapshot value {};
        rachel_audio_query(reinterpret_cast<void *>(handle), revision, &value);
        const jlong fields[] = {value.playing ? 1 : 0, value.position, value.duration, value.error};
        env->SetLongArrayRegion(output, 0, 4, fields);
    }
}
