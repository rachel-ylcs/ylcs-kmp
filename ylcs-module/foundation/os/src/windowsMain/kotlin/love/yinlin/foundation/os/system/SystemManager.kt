package love.yinlin.foundation.os.system

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.wcstr
import kotlinx.cinterop.UShortVar
import love.yinlin.extension.catchingNull
import platform.windows.FALSE
import platform.windows.GetEnvironmentVariableW
import platform.windows.GetVersionExW
import platform.windows.OSVERSIONINFOW

@OptIn(ExperimentalForeignApi::class)
actual object SystemManager {
    actual suspend fun getSystemVersion(): String? {
        val version = catchingNull {
            memScoped {
                val info = alloc<OSVERSIONINFOW>()
                info.dwOSVersionInfoSize = sizeOf<OSVERSIONINFOW>().convert()
                if (GetVersionExW(info.ptr) == FALSE) null
                else Triple(info.dwMajorVersion.toInt(), info.dwMinorVersion.toInt(), info.dwBuildNumber.toInt())
            }
        }

        return version?.let { WindowsSystemVersion.format(it.first, it.second, it.third) }
    }

    actual suspend fun getUserName(): String? = catchingNull {
        memScoped {
            val variable = "USERNAME".wcstr.ptr
            val buffer = allocArray<UShortVar>(256)
            val length = GetEnvironmentVariableW(variable, buffer, 256u)
            if (length == 0u || length >= 256u) null
            else buffer.toKStringFromUtf16().takeIf { it.isNotBlank() }
        }
    }
}
