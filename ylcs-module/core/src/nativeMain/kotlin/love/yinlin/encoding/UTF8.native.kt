package love.yinlin.encoding

internal actual fun convertUTF8(data: ByteArray): String = data.decodeToString()

internal actual fun convertUTF8(data: String): ByteArray = data.encodeToByteArray()