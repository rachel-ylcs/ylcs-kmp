package love.yinlin.platform.ffi

object Win32 {
    typealias BOOL = Int
    typealias DWORD = Int
    typealias POINTER = Address
    typealias HANDLE = Address

    const val TRUE: DWORD = 1
    const val FALSE: DWORD = 0

    const val EVENT_ALL_ACCESS: DWORD = 0x1F0003

    const val GWL_EXSTYLE: DWORD = -20

    const val WS_EX_TRANSPARENT: DWORD = 0x00000020
    const val WS_EX_TOOLWINDOW: DWORD = 0x00000080
}

object Win32Type {
    val BOOL: Layout = NativeType.Int
    val DWORD: Layout = NativeType.Int
    val POINTER: Layout = NativeType.Pointer
    val HWND: Layout = NativeType.Pointer
    val HANDLE: Layout = NativeType.Pointer
}