package love.yinlin.foundation.os.system

import java.lang.foreign.ValueLayout
import java.nio.file.Files
import java.nio.file.Path
import love.yinlin.extension.catchingNull
import love.yinlin.platform.Platform
import love.yinlin.platform.ffi.FFI
import love.yinlin.platform.ffi.Win32
import love.yinlin.platform.ffi.useMemory
import love.yinlin.platform.platform

actual object SystemManager {
    actual fun getSystemVersion(): String? = when (platform) {
        Platform.Windows -> windowsVersion()
        Platform.Linux -> linuxVersion()
        Platform.MacOS -> macOSVersion()
        else -> null
    }

    actual fun getUserName(): String? = catchingNull {
        System.getProperty("user.name")?.takeIf { it.isNotBlank() }
    }

    private fun windowsVersion(): String? {
        val nativeVersion = catchingNull {
            useMemory { arena ->
                // OSVERSIONINFOW: five DWORD fields followed by WCHAR[128].
                val info = arena.allocate(276, 4)
                info.set(ValueLayout.JAVA_INT, 0, 276)
                if ((FFI.kernel32.GetVersionExW(info) as Int) == Win32.FALSE) null
                else {
                    val major = info.get(ValueLayout.JAVA_INT, 4)
                    val minor = info.get(ValueLayout.JAVA_INT, 8)
                    val build = info.get(ValueLayout.JAVA_INT, 12)
                    formatWindowsVersion(major, minor, build)
                }
            }
        }

        if (nativeVersion != null) return nativeVersion

        val name = System.getProperty("os.name") ?: return null
        return when {
            name.contains("11") -> "Windows 11"
            name.contains("10") -> "Windows 10"
            else -> name.takeIf { it.isNotBlank() }
        }
    }

    private fun formatWindowsVersion(major: Int, minor: Int, build: Int): String = when {
        major >= 10 && build >= 22000 -> "Windows 11"
        major >= 10 -> "Windows 10"
        major == 6 && minor == 3 -> "Windows 8.1"
        major == 6 && minor == 2 -> "Windows 8"
        major == 6 && minor == 1 -> "Windows 7"
        major == 6 && minor == 0 -> "Windows Vista"
        major == 5 && minor == 1 -> "Windows XP"
        else -> "Windows " + major + "." + minor
    }

    private fun linuxVersion(): String? {
        val release = catchingNull {
            val content = Files.readString(Path.of("/etc/os-release"))
            val values = content.lineSequence().mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null
                else line.substring(0, separator) to line.substring(separator + 1).trim().removeSurrounding("\"").removeSurrounding("'")
            }.toMap()

            val name = values["NAME"]
            val version = values["VERSION_ID"]
            when {
                !name.isNullOrBlank() && !version.isNullOrBlank() -> name + " " + version
                !values["PRETTY_NAME"].isNullOrBlank() -> values["PRETTY_NAME"]
                else -> null
            }
        }

        return release ?: System.getProperty("os.version")?.takeIf { it.isNotBlank() }?.let { "Linux " + it }
    }

    private fun macOSVersion(): String? {
        val version = catchingNull {
            ProcessBuilder("/usr/bin/sw_vers", "-productVersion")
                .redirectErrorStream(true)
                .start()
                .inputStream
                .bufferedReader()
                .use { it.readText().trim() }
        }

        return version?.takeIf { it.isNotBlank() }?.let { "macOS " + it }
            ?: System.getProperty("os.version")?.takeIf { it.isNotBlank() }?.let { "macOS " + it }
    }
}
