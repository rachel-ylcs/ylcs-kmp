package love.yinlin.encoding

internal fun convertASCII(data: ByteArray): String {
    val chars = CharArray(data.size) { index ->
        val byte = data[index]
        if (byte >= 0) byte.toInt().toChar() else '\uFFFD'
    }
    return chars.concatToString()
}

internal fun convertASCII(data: String): ByteArray {
    val bytes = ByteArray(data.length)
    var input = 0
    var output = 0
    while (input < data.length) {
        val code = data[input++].code
        if (code < 0x80) bytes[output++] = code.toByte()
        else {
            if (code in 0xD800 .. 0xDBFF && input < data.length && data[input].code in 0xDC00 .. 0xDFFF) input++
            bytes[output++] = 0x3F
        }
    }
    return if (output == bytes.size) bytes else bytes.copyOf(output)
}
