#include "platform.h"

#include <AppKit/AppKit.h>

void ylcs_window_set_click_through(void *handle, bool enable) {
    NSWindow *window = (__bridge NSWindow *)handle;
    @autoreleasepool {
        [window setIgnoresMouseEvents:enable];
    }
}

extern "C" {
    JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeWindowKt_setWindowClickThrough(JNIEnv* env, jclass, jlong window, jboolean enabled) {
        ylcs_window_set_click_through((void*)window, enabled);
    }
}