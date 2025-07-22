#include "../../ylcs_jni.h"

#include <Windows.h>

extern "C" {
    JNIEXPORT void JNICALL Java_love_yinlin_platform_ActualFloatingLyrics_modifyWindow(JNIEnv* env, jobject, jlong window, jboolean clickThrough) {
        HWND hwnd = (HWND)window;
        LONG exStyle = GetWindowLong(hwnd, GWL_EXSTYLE);
        exStyle = exStyle | WS_EX_TOOLWINDOW;
        exStyle = clickThrough ? (exStyle | WS_EX_TRANSPARENT) : (exStyle & ~WS_EX_TRANSPARENT);
        SetWindowLong(hwnd, GWL_EXSTYLE, exStyle);
    }
}