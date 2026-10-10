package love.yinlin.encoding

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
internal actual fun convertUTF8(data: ByteArray): String = java.lang.String(data, Charsets.UTF_8) as String

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
internal actual fun convertUTF8(data: String): ByteArray = (data as java.lang.String).getBytes(Charsets.UTF_8)