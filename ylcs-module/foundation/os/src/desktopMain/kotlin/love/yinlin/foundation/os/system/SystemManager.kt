package love.yinlin.foundation.os.system

import java.lang.foreign.ValueLayout
import love.yinlin.extension.catchingNull
import love.yinlin.fs.File
import love.yinlin.platform.Platform
import love.yinlin.platform.ffi.NativeLibrary
import love.yinlin.platform.ffi.NativeType
import love.yinlin.platform.ffi.useMemory
import love.yinlin.platform.platform

actual object SystemManager {
    private object Windows {
        private object Ntdll : NativeLibrary("ntdll") {
            val RtlGetVersion by func { declare(NativeType.Pointer, retType = NativeType.Int) }
        }

        suspend fun getSystemVersion(): String? {
            val version = catchingNull {
                useMemory { arena ->
                    // RTL_OSVERSIONINFOW: five DWORD fields followed by WCHAR[128].
                    val info = arena.allocate(276, 4)
                    info.set(ValueLayout.JAVA_INT, 0, 276)
                    if ((Ntdll.RtlGetVersion(info) as Int) != 0) null
                    else Triple(
                        info.get(ValueLayout.JAVA_INT, 4),
                        info.get(ValueLayout.JAVA_INT, 8),
                        info.get(ValueLayout.JAVA_INT, 12),
                    )
                }
            }

            return version?.let { WindowsSystemVersion.format(it.first, it.second, it.third) }
                ?: System.getProperty("os.name")?.takeIf { it.startsWith("Windows", ignoreCase = true) }
        }

        suspend fun getUserName(): String? = System.getProperty("user.name")?.takeIf { it.isNotBlank() }
    }

    private object Linux {
        suspend fun getSystemVersion(): String? = LinuxSystemVersion.getSystemVersion()

        suspend fun getUserName(): String? = System.getProperty("user.name")?.takeIf { it.isNotBlank() }
    }

    private object MacOS {
        suspend fun getSystemVersion(): String? {
            val plist = File("/System/Library/CoreServices/SystemVersion.plist").readText()
            val version = plist?.let {
                Regex("""<key>ProductVersion</key>\s*<string>([^<]+)</string>""").find(it)?.groupValues?.get(1)
            }
            return version?.let { "macOS $it" } ?: "macOS"
        }

        suspend fun getUserName(): String? = System.getProperty("user.name")?.takeIf { it.isNotBlank() }
    }

    actual suspend fun getSystemVersion(): String? = when (platform) {
        Platform.Windows -> Windows.getSystemVersion()
        Platform.Linux -> Linux.getSystemVersion()
        Platform.MacOS -> MacOS.getSystemVersion()
        else -> null
    }

    actual suspend fun getUserName(): String? = when (platform) {
        Platform.Windows -> Windows.getUserName()
        Platform.Linux -> Linux.getUserName()
        Platform.MacOS -> MacOS.getUserName()
        else -> null
    }
}
