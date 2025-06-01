#include "ylcs_jni.h"
#include "platform.h"

extern "C" {
    JNIEXPORT void JNICALL Java_love_yinlin_platform_ActualFloatingLyrics_modifyWindow(JNIEnv* env, jobject, jlong window, jboolean clickThrough) {
        ylcs_window_set_click_through((void*)window, (bool)clickThrough);
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_MainKt_requestSingleInstance(JNIEnv* env, jobject) {
        return (jboolean)ylcs_single_instance_try_lock();
    }

    JNIEXPORT void JNICALL Java_love_yinlin_MainKt_releaseSingleInstance(JNIEnv* env, jobject) {
        ylcs_single_instance_unlock();
    }
}