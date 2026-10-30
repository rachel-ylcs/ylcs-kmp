package love.yinlin.foundation.os

import kotlinx.cinterop.ExperimentalForeignApi
import platform.windows.CloseHandle
import platform.windows.CreateEventW
import platform.windows.ERROR_ALREADY_EXISTS
import platform.windows.FALSE
import platform.windows.GetLastError
import platform.windows.HANDLE

@OptIn(ExperimentalForeignApi::class)
actual object SingleInstance {
    private var appEvent: HANDLE? = null

    actual fun run(key: String): Boolean {
        if (appEvent != null || key.isEmpty() || '\u0000' in key) return false
        val event = CreateEventW(null, FALSE, FALSE, key) ?: return false
        if (GetLastError() == ERROR_ALREADY_EXISTS.toUInt()) {
            CloseHandle(event)
            return false
        }
        appEvent = event
        return true
    }
}
