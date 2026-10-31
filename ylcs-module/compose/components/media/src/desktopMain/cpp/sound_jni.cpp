#include "rachel_audio.h"
#include <jni.h>
#include <string>
#include <vector>

extern "C" {
    JNIEXPORT jlong JNICALL Java_love_yinlin_media_SoundNative_create(JNIEnv *, jobject) {
        return reinterpret_cast<jlong>(rachel_sound_create());
    }

    JNIEXPORT void JNICALL Java_love_yinlin_media_SoundNative_release(JNIEnv *, jobject, jlong handle) {
        rachel_sound_release(reinterpret_cast<void *>(handle));
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_media_SoundNative_addPath(JNIEnv *env, jobject, jlong handle, jbyteArray path) {
        if (!path) return JNI_FALSE;
        try {
            const auto size = env->GetArrayLength(path);
            std::string value(static_cast<size_t>(size), '\0');
            env->GetByteArrayRegion(path, 0, size, reinterpret_cast<jbyte *>(value.data()));
            if (env->ExceptionCheck()) return JNI_FALSE;
            return rachel_sound_add_file(reinterpret_cast<void *>(handle), value.c_str()) ? JNI_TRUE : JNI_FALSE;
        }
        catch (...) { return JNI_FALSE; }
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_media_SoundNative_addBytes(JNIEnv *env, jobject, jlong handle, jbyteArray data) {
        if (!data) return JNI_FALSE;
        try {
            const auto size = env->GetArrayLength(data);
            if (!size) return JNI_FALSE;
            std::vector<uint8_t> bytes(size);
            env->GetByteArrayRegion(data, 0, size, reinterpret_cast<jbyte *>(bytes.data()));
            if (env->ExceptionCheck()) return JNI_FALSE;
            return rachel_sound_add_memory(reinterpret_cast<void *>(handle), bytes.data(), size) ? JNI_TRUE : JNI_FALSE;
        }
        catch (...) { return JNI_FALSE; }
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_media_SoundNative_play(JNIEnv *, jobject, jlong handle, jint index) {
        return rachel_sound_play(reinterpret_cast<void *>(handle), index) ? JNI_TRUE : JNI_FALSE;
    }
}
