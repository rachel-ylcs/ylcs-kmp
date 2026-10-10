package love.yinlin.encoding

internal fun convertStringToLatin1(data: String): ByteArray {
    val bytes = ByteArray(data.length)
    var input = 0
    var output = 0
    while (input < data.length) {
        val code = data[input++].code
        if (code <= 0xFF) bytes[output++] = code.toByte()
        else {
            if (code in 0xD800 .. 0xDBFF && input < data.length && data[input].code in 0xDC00 .. 0xDFFF) input++
            bytes[output++] = 0x3F
        }
    }
    return if (output == bytes.size) bytes else bytes.copyOf(output)
}

internal fun convertLatin1ToString(data: ByteArray): String {
    val chars = CharArray(data.size) { index ->
        (data[index].toInt() and 0xFF).toChar()
    }
    return chars.concatToString()
}
