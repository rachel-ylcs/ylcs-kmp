package love.yinlin.platform

object Win32 {
    typealias BOOL = Int
    typealias DWORD = Int
    typealias POINTER = Native.Pointer
    typealias HANDLE = Native.Pointer

    const val TRUE: DWORD = 1
    const val FALSE: DWORD = 0

    const val EVENT_ALL_ACCESS: DWORD = 0x1F0003
}

object Win32Type {
    val BOOL: Native.Type = NativeType.Int
    val DWORD: Native.Type = NativeType.Int
    val POINTER: Native.Type = NativeType.Pointer
    val HANDLE: Native.Type = NativeType.Pointer
}