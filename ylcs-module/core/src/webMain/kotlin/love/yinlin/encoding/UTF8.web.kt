package love.yinlin.encoding

private fun codePointFromSurrogate(string: String, high: Int, index: Int, endIndex: Int): Int {
    if (high !in 0xD800 .. 0xDBFF || index >= endIndex) return 0
    val low = string[index].code
    return if (low !in 0xDC00 .. 0xDFFF) 0 else 0x10000 + ((high and 0x3FF) shl 10) or (low and 0x3FF)
}

private fun codePointFrom2(bytes: ByteArray, byte1: Int, index: Int, endIndex: Int): Int {
    if (byte1 and 0x1E == 0 || index >= endIndex) return 0
    val byte2 = bytes[index].toInt()
    return if (byte2 and 0xC0 != 0x80) 0 else (byte1 shl 6) xor byte2 xor 0xF80
}

private fun codePointFrom3(bytes: ByteArray, byte1: Int, index: Int, endIndex: Int): Int {
    if (index >= endIndex) return 0
    val byte2 = bytes[index].toInt()
    if (byte1 and 0xF == 0) {
        if (byte2 and 0xE0 != 0xA0) return 0
    }
    else if (byte1 and 0xF == 0xD) {
        if (byte2 and 0xE0 != 0x80) return 0
    }
    else if (byte2 and 0xC0 != 0x80) return 0

    if (index + 1 == endIndex) return -1
    val byte3 = bytes[index + 1].toInt()
    if (byte3 and 0xC0 != 0x80) return -1
    return (byte1 shl 12) xor (byte2 shl 6) xor byte3 xor -0x1E080
}

private fun codePointFrom4(bytes: ByteArray, byte1: Int, index: Int, endIndex: Int): Int {
    if (index >= endIndex) return 0
    val byte2 = bytes[index].toInt()
    if (byte1 and 0xF == 0x0) {
        if (byte2 and 0xF0 <= 0x80) return 0
    }
    else if (byte1 and 0xF == 0x4) {
        if (byte2 and 0xF0 != 0x80) return 0
    }
    else if (byte1 and 0xF > 0x4) return 0
    if (byte2 and 0xC0 != 0x80) return 0
    if (index + 1 == endIndex) return -1
    val byte3 = bytes[index + 1].toInt()
    if (byte3 and 0xC0 != 0x80) return -1
    if (index + 2 == endIndex) return -2
    val byte4 = bytes[index + 2].toInt()
    if (byte4 and 0xC0 != 0x80) return -2
    return (byte1 shl 18) xor (byte2 shl 12) xor (byte3 shl 6) xor byte4 xor 0x381F80
}

private const val MAX_BYTES_PER_CHAR = 3

private val REPLACEMENT_BYTE_SEQUENCE: ByteArray = [0xEF.toByte(), 0xBF.toByte(), 0xBD.toByte()]

private const val REPLACEMENT_CHAR = '\uFFFD'

internal actual fun convertStringToUTF8(data: String): ByteArray {
    val length = data.length
    val bytes = ByteArray(length * MAX_BYTES_PER_CHAR)
    var byteIndex = 0
    var charIndex = 0

    while (charIndex < length) {
        val code = data[charIndex++].code
        when {
            code < 0x80 -> bytes[byteIndex++] = code.toByte()
            code < 0x800 -> {
                bytes[byteIndex++] = ((code shr 6) or 0xC0).toByte()
                bytes[byteIndex++] = ((code and 0x3F) or 0x80).toByte()
            }
            code !in 0xD800 ..< 0xE000 -> {
                bytes[byteIndex++] = ((code shr 12) or 0xE0).toByte()
                bytes[byteIndex++] = (((code shr 6) and 0x3F) or 0x80).toByte()
                bytes[byteIndex++] = ((code and 0x3F) or 0x80).toByte()
            }
            else -> {
                val codePoint = codePointFromSurrogate(data, code, charIndex, length)
                if (codePoint <= 0) {
                    bytes[byteIndex++] = REPLACEMENT_BYTE_SEQUENCE[0]
                    bytes[byteIndex++] = REPLACEMENT_BYTE_SEQUENCE[1]
                    bytes[byteIndex++] = REPLACEMENT_BYTE_SEQUENCE[2]
                }
                else {
                    bytes[byteIndex++] = ((codePoint shr 18) or 0xF0).toByte()
                    bytes[byteIndex++] = (((codePoint shr 12) and 0x3F) or 0x80).toByte()
                    bytes[byteIndex++] = (((codePoint shr 6) and 0x3F) or 0x80).toByte()
                    bytes[byteIndex++] = ((codePoint and 0x3F) or 0x80).toByte()
                    charIndex++
                }
            }
        }
    }

    return if (bytes.size == byteIndex) bytes else bytes.copyOf(byteIndex)
}

internal actual fun convertUTF8ToString(data: ByteArray): String {
    val length = data.size
    var byteIndex = 0
    return buildString {
        while (byteIndex < length) {
            val byte = data[byteIndex++].toInt()
            when {
                byte >= 0 -> append(byte.toChar())
                byte shr 5 == -2 -> {
                    val code = codePointFrom2(data, byte, byteIndex, length)
                    if (code <= 0) {
                        append(REPLACEMENT_CHAR)
                        byteIndex += -code
                    }
                    else {
                        append(code.toChar())
                        byteIndex += 1
                    }
                }
                byte shr 4 == -2 -> {
                    val code = codePointFrom3(data, byte, byteIndex, length)
                    if (code <= 0) {
                        append(REPLACEMENT_CHAR)
                        byteIndex += -code
                    } else {
                        append(code.toChar())
                        byteIndex += 2
                    }
                }
                byte shr 3 == -2 -> {
                    val code = codePointFrom4(data, byte, byteIndex, length)
                    if (code <= 0) {
                        append(REPLACEMENT_CHAR)
                        byteIndex += -code
                    }
                    else {
                        val high = (code - 0x10000) shr 10 or 0xD800
                        val low = (code and 0x3FF) or 0xDC00
                        append(high.toChar())
                        append(low.toChar())
                        byteIndex += 3
                    }
                }
                else -> append(REPLACEMENT_CHAR)
            }
        }
    }
}
