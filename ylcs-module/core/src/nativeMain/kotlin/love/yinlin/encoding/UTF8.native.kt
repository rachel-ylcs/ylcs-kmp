package love.yinlin.encoding

internal actual fun convertStringToUTF8(data: String): ByteArray = data.encodeToByteArray()

internal actual fun convertUTF8ToString(data: ByteArray): String = data.decodeToString()
