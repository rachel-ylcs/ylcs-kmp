package love.yinlin.encoding

private val ConvertMap = [
    ::convertStringToUTF8 to ::convertUTF8ToString,
    ::convertStringToASCII to ::convertASCIIToString,
    ::convertStringToLatin1 to ::convertLatin1ToString,
    ::convertStringToGBK to ::convertGBKToString,
    ::convertStringToUTF16LE to ::convertUTF16LEToString,
    ::convertStringToUTF16BE to ::convertUTF16BEToString,
    ::convertStringToUTF32LE to ::convertUTF32LEToString,
    ::convertStringToUTF32BE to ::convertUTF32BEToString,
]

fun String.convert(encoding: Encoding): ByteArray = ConvertMap[encoding.ordinal].first(this)

fun ByteArray.convert(encoding: Encoding): String = ConvertMap[encoding.ordinal].second(this)

internal expect fun convertStringToUTF8(data: String): ByteArray
internal expect fun convertUTF8ToString(data: ByteArray): String
