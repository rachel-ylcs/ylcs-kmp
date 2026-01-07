package love.yinlin.platform

internal object WindowsSingleInstance : SingleInstance, NativeLibrary("kernel32") {
    val CreateEventW by func(
        Win32Type.POINTER,
        Win32Type.BOOL,
        Win32Type.BOOL,
        NativeType.wstring,
        retType = Win32Type.HANDLE,
    )

    val OpenEventW by func(
        Win32Type.DWORD,
        Win32Type.BOOL,
        NativeType.wstring,
        retType = Win32Type.HANDLE,
    )

    val CloseHandle by func(
        Win32Type.HANDLE,
        retType = Win32Type.BOOL,
    )

    var appEvent: Win32.HANDLE = Native.NULL

    override fun lock(key: String): Boolean = useMemory { arena ->
        appEvent = OpenEventW(Win32.EVENT_ALL_ACCESS, Win32.FALSE, arena.wString(key)) as Win32.HANDLE
        if (appEvent.isNull) {
            appEvent = CreateEventW(Native.NULL, Win32.FALSE, Win32.FALSE, arena.wString(key)) as Win32.HANDLE
            true
        }
        else false
    }

    override fun unlock() {
        if (appEvent.isNotNull) {
            CloseHandle(appEvent)
            appEvent = Native.NULL
        }
    }
}