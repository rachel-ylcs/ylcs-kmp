package org.jetbrains.compose.resources

internal actual fun getPlatformResourceReader(): ResourceReader = DesktopResourceReader

/** Reads the deployed resource tree relative to the application, independently of the working directory. */
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
        require(path.isNotEmpty() && '\u0000' !in path && '\\' !in path && ':' !in path && !path.startsWith('/')) { "Resource path must be relative to the application" }
        require(path.split('/').all { it.isNotEmpty() && it != "." && it != ".." }) { "Resource path contains an invalid segment" }
        return "$baseDirectory/$path"
    }
}
