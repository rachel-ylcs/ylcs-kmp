package love.yinlin.platform

import love.yinlin.extension.cleaning
import love.yinlin.platform.ffi.*

private object WindowsNativeWindow {
    fun updateClickThrough(handle: Long, enabled: Boolean) {
        val hwnd = handle.asAddress
        var exStyle = FFI.user32.GetWindowLongW(hwnd, Win32.GWL_EXSTYLE) as Int
        exStyle = exStyle or Win32.WS_EX_TOOLWINDOW
        exStyle = if (enabled) exStyle or Win32.WS_EX_TRANSPARENT else exStyle and Win32.WS_EX_TRANSPARENT.inv()
        FFI.user32.SetWindowLongW(hwnd, Win32.GWL_EXSTYLE, exStyle)
    }
}

private object MacOSNativeWindow {
    fun updateClickThrough(handle: Long, enabled: Boolean) {
        val pool = FFI.objc.objc_autoreleasePoolPush()
        cleaning({
            val windowPtr = Address.ofAddress(handle)
            useMemory { arena ->
                val selName = arena.allocateFrom("setIgnoresMouseEvents:")
                val sel = FFI.objc.sel_registerName(selName) as Address
                FFI.objc.objcVoidMessageWithBoolean(windowPtr, sel, enabled)
            }
        }) {
            FFI.objc.objc_autoreleasePoolPop(pool)
        }
    }
}

internal fun updateClickThrough(handle: Long, enabled: Boolean) {
    when (platform) {
        Platform.Windows -> WindowsNativeWindow.updateClickThrough(handle, enabled)
        Platform.Linux -> { }
        Platform.MacOS -> MacOSNativeWindow.updateClickThrough(handle, enabled)
        else -> { }
    }
}
