#include <jni.h>
#include <webp/types.h>
#include <webp/decode.h>

extern "C" {
    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
    {
        JNIEnv* env = nullptr;
        if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) return -1;
        return JNI_VERSION_1_6;
    }

    JNIEXPORT jint JNICALL Java_love_yinlin_libwebpJNI_WebPGetEncoderVersion(JNIEnv *jenv, jclass jcls) {
      return (jint)WebPGetDecoderVersion();
    }
}