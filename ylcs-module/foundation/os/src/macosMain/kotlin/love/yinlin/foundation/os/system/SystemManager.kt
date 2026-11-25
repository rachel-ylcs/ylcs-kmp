package love.yinlin.foundation.os.system

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import love.yinlin.extension.catchingNull
import platform.Foundation.NSProcessInfo
import platform.posix.geteuid
import platform.posix.getpwuid

@OptIn(ExperimentalForeignApi::class)
actual object SystemManager {
    actual suspend fun getSystemVersion(): String? = catchingNull {
        val rawVersion = NSProcessInfo.processInfo.operatingSystemVersionString
        val version = rawVersion.substringAfter("Version ", "").substringBefore(' ')
        version.takeIf { it.isNotBlank() }?.let { "macOS $it" }
    }

    actual suspend fun getUserName(): String? = catchingNull {
        getpwuid(geteuid())?.pointed?.pw_name?.toKString()?.takeIf { it.isNotBlank() }
    }
}
