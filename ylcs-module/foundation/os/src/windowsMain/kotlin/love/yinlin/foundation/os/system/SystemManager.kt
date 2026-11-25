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
    actual fun getSystemVersion(): String? = catchingNull {
        memScoped {
            val info = alloc<OSVERSIONINFOW>()
            info.dwOSVersionInfoSize = sizeOf<OSVERSIONINFOW>().convert()
            if (GetVersionExW(info.ptr) == FALSE) return@memScoped null

            val major = info.dwMajorVersion.toInt()
            val minor = info.dwMinorVersion.toInt()
            val build = info.dwBuildNumber.toInt()
            when {
                major >= 10 && build >= 22000 -> "Windows 11"
                major >= 10 -> "Windows 10"
                major == 6 && minor == 3 -> "Windows 8.1"
                major == 6 && minor == 2 -> "Windows 8"
                major == 6 && minor == 1 -> "Windows 7"
                major == 6 && minor == 0 -> "Windows Vista"
                major == 5 && minor == 1 -> "Windows XP"
                else -> "Windows " + major + "." + minor
            }
        }
    }

    actual fun getUserName(): String? = catchingNull {
        memScoped {
            val variable = "USERNAME".wcstr.ptr
            val buffer = allocArray<UShortVar>(256)
            val length = GetEnvironmentVariableW(variable, buffer, 256u)
            if (length == 0u || length >= 256u) null
            else buffer.toKStringFromUtf16().takeIf { it.isNotBlank() }
        }
    }
}
