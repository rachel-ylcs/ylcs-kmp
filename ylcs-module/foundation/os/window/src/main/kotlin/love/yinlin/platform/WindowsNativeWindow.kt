package love.yinlin.platform

import love.yinlin.platform.ffi.Native
import love.yinlin.platform.ffi.NativeLibrary
import love.yinlin.platform.ffi.NativeType
import love.yinlin.platform.ffi.Win32
import love.yinlin.platform.ffi.Win32Type

internal object WindowsNativeWindow : NativeWindowImpl, NativeLibrary("user32") {
    val GetWindowLongW by func(
        Win32Type.HWND,
        NativeType.Int,
        retType = NativeType.Int
    )

    val SetWindowLongW by func(
        Win32Type.HWND,
        NativeType.Int,
        NativeType.Int,
        retType = NativeType.Int
    )

    override fun updateClickThrough(handle: Long, enabled: Boolean) {
        val hwnd = Native.Pointer.ofAddress(handle)
        var exStyle = GetWindowLongW(hwnd, Win32.GWL_EXSTYLE) as Int
        exStyle = exStyle or Win32.WS_EX_TOOLWINDOW
        exStyle = if (enabled) exStyle or Win32.WS_EX_TRANSPARENT else exStyle and Win32.WS_EX_TRANSPARENT.inv()
        SetWindowLongW(hwnd, Win32.GWL_EXSTYLE, exStyle)
    }
}