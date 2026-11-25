package love.yinlin.encoding

internal expect fun convertStringToUTF8(data: String): ByteArray
internal expect fun convertUTF8ToString(data: ByteArray): String
