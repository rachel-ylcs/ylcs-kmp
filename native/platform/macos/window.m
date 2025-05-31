#include "platform.h"

#include <AppKit/AppKit.h>

void ylcs_window_set_click_through(void *handle, bool enable) {
    NSWindow *window = (__bridge NSWindow *)handle;
    @autoreleasepool {
        [window setIgnoresMouseEvents:enable];
    }
}