package love.yinlin.platform.ffi

@Suppress("PropertyName")
open class User32Library : NativeLibrary("user32") {
    val GetWindowLongW by func(Win32Type.HWND, NativeType.Int, retType = NativeType.Int)
    val SetWindowLongW by func(Win32Type.HWND, NativeType.Int, NativeType.Int, retType = NativeType.Int)
}