package love.yinlin.platform

import love.yinlin.platform.ffi.Address
import love.yinlin.platform.ffi.Kernel32Library
import love.yinlin.platform.ffi.Win32

internal object WindowsSingleInstance : SingleInstanceImpl, Kernel32Library() {
    var appEvent: Win32.HANDLE = Address.NULL

    override fun lock(key: String): Boolean = useMemory { arena ->
        appEvent = OpenEventW(Win32.EVENT_ALL_ACCESS, Win32.FALSE, arena.wstr(key)) as Win32.HANDLE
        if (appEvent.isNull) {
            appEvent = CreateEventW(Address.NULL, Win32.FALSE, Win32.FALSE, arena.wstr(key)) as Win32.HANDLE
            true
        }
        else false
    }

    override fun unlock() {
        if (appEvent.isNotNull) {
            CloseHandle(appEvent)
            appEvent = Address.NULL
        }
    }
}