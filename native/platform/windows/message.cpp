#include "../../ylcs_jni.h"

#include <Windows.h>

static HANDLE appEvent = nullptr;

extern "C" {
    JNIEXPORT jboolean JNICALL Java_love_yinlin_platform_OS_1desktopKt_requestSingleInstance(JNIEnv* env, jclass) {
        LPCWSTR name = L"ylcs-desktop";
        appEvent = OpenEventW(EVENT_ALL_ACCESS, FALSE, name);
        if (appEvent == nullptr) {
            appEvent = CreateEventW(nullptr, FALSE, FALSE, name);
            return JNI_TRUE;
        }
        return JNI_FALSE;
    }

    JNIEXPORT void JNICALL Java_love_yinlin_platform_OS_1desktopKt_releaseSingleInstance(JNIEnv* env, jclass) {
        if (appEvent != nullptr) {
            CloseHandle(appEvent);
            appEvent = nullptr;
        }
    }
}