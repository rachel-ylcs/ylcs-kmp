package love.yinlin.encoding

import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

private fun convertStringToUTF32(data: String, endian: Endian): ByteArray {
    var count = data.length
    var input = 0
    while (input < data.length) {
        val code = data[input++].code
        if (code in 0xD800 .. 0xDBFF && input < data.length && data[input].code in 0xDC00 .. 0xDFFF) {
            input++
            count--
        }
    }
    return ByteArrayIO(count * 4).write {
        input = 0
        var output = 0
        while (input < data.length) {
            var code = data[input++].code
            when (code) {
                in 0xD800 .. 0xDBFF -> {
                    code = if (input < data.length && data[input].code in 0xDC00 .. 0xDFFF) {
                        0x10000 + ((code - 0xD800) shl 10) + (data[input++].code - 0xDC00)
                    }
                    else 0xFFFD
                }
                in 0xDC00 .. 0xDFFF -> code = 0xFFFD
            }
            writeInt(output, code, endian)
            output += 4
        }
    }
}

private fun convertUTF32ToString(data: ByteArray, endian: Endian): String {
    val io = ByteArrayIO(data)
    val end = data.size and -4
    val chars = CharArray(data.size / 4 * 2 + if (data.size and 3 == 0) 0 else 1)
    var input = 0
    var output = 0
    while (input < end) {
        val code = io.readInt(input, endian)
        input += 4
        when {
            code !in 0 .. 0x10FFFF || code in 0xD800 .. 0xDFFF -> chars[output++] = '\uFFFD'
            code < 0x10000 -> chars[output++] = code.toChar()
            else -> {
                val value = code - 0x10000
                chars[output++] = (0xD800 + (value ushr 10)).toChar()
                chars[output++] = (0xDC00 + (value and 0x3FF)).toChar()
            }
        }
    }
    if (input < data.size) chars[output++] = '\uFFFD'
    return chars.concatToString(0, output)
}

internal fun convertStringToUTF32LE(data: String): ByteArray = convertStringToUTF32(data, Endian.LITTLE)
internal fun convertUTF32LEToString(data: ByteArray): String = convertUTF32ToString(data, Endian.LITTLE)

internal fun convertStringToUTF32BE(data: String): ByteArray = convertStringToUTF32(data, Endian.BIG)
internal fun convertUTF32BEToString(data: ByteArray): String = convertUTF32ToString(data, Endian.BIG)
