package love.yinlin.encoding

fun String.convert(encoding: Encoding): ByteArray = when (encoding) {
    Encoding.UTF8 -> convertUTF8(this)
    Encoding.GBK -> convertGBK(this)
}

fun ByteArray.convert(encoding: Encoding): String = when (encoding) {
    Encoding.UTF8 -> convertUTF8(this)
    Encoding.GBK -> convertGBK(this)
}

internal expect fun convertUTF8(data: ByteArray): String
internal expect fun convertUTF8(data: String): ByteArray
