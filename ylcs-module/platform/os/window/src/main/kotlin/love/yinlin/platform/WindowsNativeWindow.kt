package love.yinlin.platform

import love.yinlin.platform.ffi.User32Library
import love.yinlin.platform.ffi.Win32

internal object WindowsNativeWindow : NativeWindowImpl, User32Library() {
    override fun updateClickThrough(handle: Long, enabled: Boolean) {
        val hwnd = handle.asAddress
        var exStyle = GetWindowLongW(hwnd, Win32.GWL_EXSTYLE) as Int
        exStyle = exStyle or Win32.WS_EX_TOOLWINDOW
        exStyle = if (enabled) exStyle or Win32.WS_EX_TRANSPARENT else exStyle and Win32.WS_EX_TRANSPARENT.inv()
        SetWindowLongW(hwnd, Win32.GWL_EXSTYLE, exStyle)
    }
}