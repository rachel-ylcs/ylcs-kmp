package love.yinlin.platform

import love.yinlin.extension.cleaning
import love.yinlin.platform.ffi.*

private object WindowsNativeWindow : User32Library() {
    fun updateClickThrough(handle: Long, enabled: Boolean) {
        val hwnd = handle.asAddress
        var exStyle = GetWindowLongW(hwnd, Win32.GWL_EXSTYLE) as Int
        exStyle = exStyle or Win32.WS_EX_TOOLWINDOW
        exStyle = if (enabled) exStyle or Win32.WS_EX_TRANSPARENT else exStyle and Win32.WS_EX_TRANSPARENT.inv()
        SetWindowLongW(hwnd, Win32.GWL_EXSTYLE, exStyle)
    }
}

private object MacOSNativeWindow : NativeLibrary("/usr/lib/libobjc.A.dylib") {
    val sel_registerName by func(NativeType.Pointer, retType = NativeType.Pointer)
    val objc_msgSend by func(NativeType.Pointer, NativeType.Pointer, NativeType.Boolean)
    val objc_autoreleasePoolPush by func(retType = NativeType.Pointer)
    val objc_autoreleasePoolPop by func(NativeType.Pointer)

    fun updateClickThrough(handle: Long, enabled: Boolean) {
        val pool = objc_autoreleasePoolPush()
        cleaning({
            val windowPtr = Address.ofAddress(handle)
            useMemory { arena ->
                val selName = arena.allocateFrom("setIgnoresMouseEvents:")
                val sel = sel_registerName(selName) as Address
                objc_msgSend(windowPtr, sel, enabled)
            }
        }) {
            objc_autoreleasePoolPop(pool)
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