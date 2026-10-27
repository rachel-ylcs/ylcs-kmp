package org.jetbrains.compose.resources

import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs

private val WindowsDeviceNames: Set<String> = ["CON", "PRN", "AUX", "NUL", "CONIN$", "CONOUT$"]

internal actual fun getPlatformResourceReader(): ResourceReader = DesktopResourceReader

private object DesktopResourceReader : ResourceReader {
    private val baseDirectory: String by lazy {
        resourceBaseDirectory().replace('\\', '/').trimEnd('/')
    }

    override suspend fun read(path: String): ByteArray = readResourceFile(resolve(path), 0, null)

    override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray {
        require(offset >= 0 && size >= 0) { "Resource offset and size must be non-negative" }
        return readResourceFile(resolve(path), offset, size)
    }

    override fun getUri(path: String): String {
        val absolutePath = resolve(path)
        if (!resourceFileExists(absolutePath)) throw MissingResourceException(path)

        val encoded = buildString {
            for (byte in absolutePath.encodeToByteArray()) {
                val value = byte.toInt() and 0xFF
                if (value in 'a'.code .. 'z'.code || value in 'A'.code .. 'Z'.code || value in '0'.code .. '9'.code || value.toChar() in "/:-._~") append(value.toChar())
                else {
                    append('%')
                    append("0123456789ABCDEF"[value ushr 4])
                    append("0123456789ABCDEF"[value and 0xF])
                }
            }
        }

        return when {
            encoded.startsWith("//") -> "file:$encoded"
            encoded.startsWith('/') -> "file://$encoded"
            else -> "file:///$encoded"
        }
    }

    private fun resolve(path: String): String {
        validateDesktopResourcePath(path, hostOs == OS.Windows)
        return "$baseDirectory/$path"
    }
}

internal fun validateDesktopResourcePath(path: String, windows: Boolean) {
    require(path.isNotEmpty() && '\u0000' !in path && '\\' !in path && ':' !in path && !path.startsWith('/')) { "Resource path must be relative to the application" }

    for (segment in path.split('/')) {
        require(segment.isNotEmpty() && segment != "." && segment != "..") { "Resource path contains an invalid segment" }

        if (windows) {
            require(segment.last() != ' ' && segment.last() != '.' && segment.none { it < ' ' || it in "<>\"|?*" }) { "Resource path contains a Windows-normalized segment" }

            val name = segment.substringBefore('.').trimEnd(' ').uppercase()
            val device = name in WindowsDeviceNames || (name.startsWith("COM") || name.startsWith("LPT")) && name.length == 4 && name.last() in "123456789¹²³"
            require(!device) { "Resource path refers to a Windows device" }
        }
    }
}
