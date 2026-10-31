#include "media_jni.h"

extern "C" {
    JNIEXPORT jlong JNICALL Java_love_yinlin_media_VideoNative_createPlayer(JNIEnv *env, jobject, jobject callback) {
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
        return reinterpret_cast<jlong>(rachel_video_create(JvmAudioCallbacks::notify, JvmAudioCallbacks::destroy, listener));
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_media_VideoNative_readInfo(JNIEnv *env, jobject, jlong handle, jlong revision, jlongArray output) {
        if (!output || env->GetArrayLength(output) < 3) return JNI_FALSE;
        RachelVideoFrameInfo info {};
        if (!rachel_video_info(reinterpret_cast<void *>(handle), revision, &info)) return JNI_FALSE;
        const jlong fields[] = {info.width, info.height, info.serial};
        env->SetLongArrayRegion(output, 0, 3, fields);
        return env->ExceptionCheck() ? JNI_FALSE : JNI_TRUE;
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_media_VideoNative_copyFrame(JNIEnv *, jobject, jlong handle, jlong revision, jlong pixels, jlong capacity, jint width, jint height) {
        return rachel_video_copy_frame(reinterpret_cast<void *>(handle), revision, reinterpret_cast<void *>(pixels), capacity, width, height) ? JNI_TRUE : JNI_FALSE;
    }
}
