package love.yinlin.foundation.os.process

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.wcstr
import love.yinlin.platform.platform
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
        return memScoped {
            val name = "$key.${platform.name}".wcstr.ptr
            // 必须先拿到指针, 否则 LLVM IR 可能会插入创建字符串时的 Error 从而打断 CreateEventW 的 GetLastError
            val event = CreateEventW(null, FALSE, FALSE, name) ?: return@memScoped false
            if (GetLastError() == ERROR_ALREADY_EXISTS.toUInt()) {
                CloseHandle(event)
                return@memScoped false
            }
            appEvent = event
            true
        }
    }
}
