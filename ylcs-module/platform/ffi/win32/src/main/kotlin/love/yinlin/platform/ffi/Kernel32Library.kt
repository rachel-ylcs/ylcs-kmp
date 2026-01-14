package love.yinlin.platform.ffi

@Suppress("PropertyName")
open class Kernel32Library : NativeLibrary("kernel32") {
    val CreateEventW by func(
        Win32Type.POINTER,
        Win32Type.BOOL,
        Win32Type.BOOL,
        NativeType.WString,
        retType = Win32Type.HANDLE,
    )

    val OpenEventW by func(
        Win32Type.DWORD,
        Win32Type.BOOL,
        NativeType.WString,
        retType = Win32Type.HANDLE,
    )

    val CloseHandle by func(
        Win32Type.HANDLE,
        retType = Win32Type.BOOL,
    )
}