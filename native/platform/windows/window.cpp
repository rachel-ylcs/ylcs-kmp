#include "platform.h"

#include <Windows.h>

void ylcs_window_set_click_through(void *handle, bool enable) {
    HWND hwnd = (HWND)handle;
    LONG exStyle = GetWindowLong(hwnd, GWL_EXSTYLE);
    exStyle = exStyle | WS_EX_TOOLWINDOW;
    exStyle = enable ? (exStyle | WS_EX_TRANSPARENT) : (exStyle & ~WS_EX_TRANSPARENT);
    SetWindowLong(hwnd, GWL_EXSTYLE, exStyle);
}
