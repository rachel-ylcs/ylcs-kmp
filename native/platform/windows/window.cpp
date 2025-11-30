#include "ylcs_jni.h"

#include <Windows.h>

extern "C" {
    JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeWindowKt_setWindowClickThrough(JNIEnv* env, jclass, jlong window, jboolean enabled) {
        HWND hwnd = (HWND)window;
        LONG exStyle = GetWindowLong(hwnd, GWL_EXSTYLE);
        exStyle = exStyle | WS_EX_TOOLWINDOW;
        exStyle = enabled ? (exStyle | WS_EX_TRANSPARENT) : (exStyle & ~WS_EX_TRANSPARENT);
        SetWindowLong(hwnd, GWL_EXSTYLE, exStyle);
    }
}