package love.yinlin.foundation.os.system

import kotlinx.cinterop.*
import love.yinlin.extension.catchingNull
import platform.windows.*

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

        return version?.let { [major, minor, build] ->
            WindowsSystemReader.formatSystemVersion(major, minor, build)
        }
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
