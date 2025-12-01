#include "ylcs_jni.h"

#include <Windows.h>

static HANDLE appEvent = nullptr;

extern "C" {
    JNIEXPORT jboolean JNICALL Java_love_yinlin_platform_SingleInstanceKt_lockApplication(JNIEnv* env, jclass, jstring key) {
        std::wstring name = j2w(env, key);
        appEvent = OpenEventW(EVENT_ALL_ACCESS, FALSE, name.data());
        if (appEvent == nullptr) {
            appEvent = CreateEventW(nullptr, FALSE, FALSE, name.data());
            return JNI_TRUE;
        }
        return JNI_FALSE;
    }

    JNIEXPORT void JNICALL Java_love_yinlin_platform_SingleInstanceKt_unlockApplication(JNIEnv* env, jclass) {
        if (appEvent != nullptr) {
            CloseHandle(appEvent);
            appEvent = nullptr;
        }
    }
}