package love.yinlin.encoding

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
internal actual fun convertStringToUTF8(data: String): ByteArray = (data as java.lang.String).getBytes(Charsets.UTF_8)

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
internal actual fun convertUTF8ToString(data: ByteArray): String = java.lang.String(data, Charsets.UTF_8) as String
