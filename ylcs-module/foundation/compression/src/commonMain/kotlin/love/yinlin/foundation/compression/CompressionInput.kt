package love.yinlin.foundation.compression

import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

internal class CompressionInput(val bytes: ByteArray, var position: Int = 0, val end: Int = bytes.size) {
    private val io = ByteArrayIO(bytes)
    val remaining: Int get() = end - position

    fun requireBytes(count: Int) {
        if (count !in 0 .. remaining) throw CompressionTruncatedInputException("Truncated compressed data", position)
    }

    fun readByte(): Int {
        requireBytes(1)
        return io.readUByte(position++).toInt()
    }

    fun readShortLE(): Int {
        requireBytes(2)
        val result = io.readUShort(position, Endian.LITTLE).toInt()
        position += 2
        return result
    }

    fun readIntLE(): Int {
        requireBytes(4)
        val result = io.readInt(position, Endian.LITTLE)
        position += 4
        return result
    }

    fun readIntBE(): Int {
        requireBytes(4)
        val result = io.readInt(position, Endian.BIG)
        position += 4
        return result
    }

    fun skip(count: Int) {
        requireBytes(count)
        position += count
    }

    fun copyTo(output: CompressionOutput, count: Int) {
        requireBytes(count)
        output.write(bytes, position, count)
        position += count
    }

    fun readVarInt(): Int {
        var value = 0
        var shift = 0
        while (shift <= 28) {
            val byte = readByte()
            if (shift == 28 && byte and 0xF8 != 0) throw CompressionInvalidDataException("Length exceeds Int range", position - 1)
            value = value or ((byte and 0x7F) shl shift)
            if (byte and 0x80 == 0) return value
            shift += 7
        }
        throw CompressionInvalidDataException("Invalid variable length integer", position)
    }
}