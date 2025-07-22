#include "platform.h"

#include <AppKit/AppKit.h>

void ylcs_window_set_click_through(void *handle, bool enable) {
    NSWindow *window = (__bridge NSWindow *)handle;
    @autoreleasepool {
        [window setIgnoresMouseEvents:enable];
    }
}

extern "C" {
    JNIEXPORT void JNICALL Java_love_yinlin_platform_ActualFloatingLyrics_modifyWindow(JNIEnv* env, jobject, jlong window, jboolean clickThrough) {
        ylcs_window_set_click_through((void*)window, clickThrough);
    }
}