package love.yinlin.foundation.os.system

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import love.yinlin.extension.catchingNull
import platform.posix.geteuid
import platform.posix.getpwuid

@OptIn(ExperimentalForeignApi::class)
actual object SystemManager {
    actual fun getSystemVersion(): String? {
        val release = catchingNull {
            SystemFileSystem.source(Path("/etc/os-release")).buffered().use { it.readString() }
        } ?: return linuxKernelVersion()

        val values = release.lineSequence().mapNotNull { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) null
            else line.substring(0, separator) to line.substring(separator + 1).trim().removeSurrounding("\"").removeSurrounding("'")
        }.toMap()

        val name = values["NAME"]
        val version = values["VERSION_ID"]
        return when {
            !name.isNullOrBlank() && !version.isNullOrBlank() -> name + " " + version
            !values["PRETTY_NAME"].isNullOrBlank() -> values["PRETTY_NAME"]
            else -> linuxKernelVersion()
        }
    }

    actual fun getUserName(): String? = catchingNull {
        getpwuid(geteuid())?.pointed?.pw_name?.toKString()?.takeIf { it.isNotBlank() }
    }

    private fun linuxKernelVersion(): String? = catchingNull {
        SystemFileSystem.source(Path("/proc/sys/kernel/osrelease")).buffered().use { "Linux " + it.readString().trim() }
    }
}
