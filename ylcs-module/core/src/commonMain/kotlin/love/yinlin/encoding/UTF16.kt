package love.yinlin.encoding

import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

private fun convertStringToUTF16(data: String, endian: Endian): ByteArray = ByteArrayIO(data.length * 2).write {
    for (index in data.indices) {
        var code = data[index].code
        when (code) {
            in 0xD800 .. 0xDBFF if (index + 1 == data.length || data[index + 1].code !in 0xDC00 .. 0xDFFF) -> code = 0xFFFD
            in 0xDC00 .. 0xDFFF if (index == 0 || data[index - 1].code !in 0xD800 .. 0xDBFF) -> code = 0xFFFD
        }
        writeShort(index * 2, code.toShort(), endian)
    }
}

private fun convertUTF16ToString(data: ByteArray, endian: Endian): String {
    val io = ByteArrayIO(data)
    val end = data.size and -2
    val chars = CharArray(data.size / 2 + (data.size and 1))
    var input = 0
    var output = 0
    while (input < end) {
        val code = io.readUShort(input, endian).toInt()
        input += 2
        when (code) {
            in 0xD800 .. 0xDBFF -> {
                if (input < end) {
                    val next = io.readUShort(input, endian).toInt()
                    if (next in 0xDC00 .. 0xDFFF) {
                        chars[output++] = code.toChar()
                        chars[output++] = next.toChar()
                        input += 2
                    }
                    else chars[output++] = '\uFFFD'
                }
                else chars[output++] = '\uFFFD'
            }
            in 0xDC00 .. 0xDFFF -> chars[output++] = '\uFFFD'
            else -> chars[output++] = code.toChar()
        }
    }
    if (input < data.size) chars[output] = '\uFFFD'
    return chars.concatToString()
}

internal fun convertStringToUTF16LE(data: String): ByteArray = convertStringToUTF16(data, Endian.LITTLE)
internal fun convertUTF16LEToString(data: ByteArray): String = convertUTF16ToString(data, Endian.LITTLE)

internal fun convertStringToUTF16BE(data: String): ByteArray = convertStringToUTF16(data, Endian.BIG)
internal fun convertUTF16BEToString(data: ByteArray): String = convertUTF16ToString(data, Endian.BIG)
