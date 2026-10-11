package love.yinlin.foundation.compression

import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

/** Snappy 原始块格式（含 varint 原始长度，非 Framed Snappy）。 */
class Snappy(val maxDecompressedSize: Int = CompressionCodec.DEFAULT_DECOMPRESSION_LIMIT) : CompressionCodec {
    init {
        CompressionCodec.checkDecompressionLimit(maxDecompressedSize)
    }

    override fun maxCompressedSize(sourceSize: Int): Int = CompressionCodec.compressedBound(sourceSize, sourceSize.toLong() / 6 + 32)

    private fun writeLiteral(output: CompressionOutput, data: ByteArray, begin: Int, length: Int) {
        if (length == 0) return
        val value = length - 1
        if (length <= 60) output.writeByte(value shl 2)
        else {
            val byteCount = (32 - value.countLeadingZeroBits() + 7) / 8
            output.writeByte((59 + byteCount) shl 2)
            var remaining = value
            repeat(byteCount) {
                output.writeByte(remaining)
                remaining = remaining ushr 8
            }
        }
        output.write(data, begin, length)
    }

    private fun writeCopy(output: CompressionOutput, distance: Int, length: Int) {
        var remaining = length
        while (remaining > 0) {
            val count = minOf(remaining, 64)
            if (count in 4..11 && distance < 2048) {
                output.writeByte(1 or ((count - 4) shl 2) or ((distance ushr 8) shl 5))
                output.writeByte(distance)
            }
            else {
                output.writeByte(2 or ((count - 1) shl 2))
                output.writeShortLE(distance)
            }
            remaining -= count
        }
    }

    override fun compress(data: ByteArray): ByteArray  {
        val output = ByteArray(maxCompressedSize(data.size))
        val size = compressPlaceable(data, output)
        return if (size == output.size) output else output.copyOf(size)
    }

    override fun compressPlaceable(src: ByteArray, dst: ByteArray, dstOffset: Int): Int = CompressionOutput.writeInto(src, dst, dstOffset) { output ->
        val io = ByteArrayIO(src)
        output.writeVarInt(src.size)
        var position = 0
        var anchor = 0
        if (src.size >= 4) {
            val table = IntArray(65536) { -1 }
            val searchLimit = src.size - 4
            while (position <= searchLimit) {
                val hash = (io.readInt(position, Endian.LITTLE) * -1640531535) ushr 16
                val candidate = table[hash]
                table[hash] = position
                if (candidate < 0 || position - candidate > 65535 || io.readInt(candidate, Endian.LITTLE) != io.readInt(position, Endian.LITTLE)) {
                    position++
                    continue
                }
                writeLiteral(output, src, anchor, position - anchor)
                var length = 4
                while (length < src.size - position && src[candidate + length] == src[position + length]) length++
                writeCopy(output, position - candidate, length)
                position += length
                anchor = position
                if (position - 1 <= searchLimit) {
                    val pe = (io.readInt(position - 1, Endian.LITTLE) * -1640531535) ushr 16
                    table[pe] = position - 1
                }
            }
        }
        writeLiteral(output, src, anchor, src.size - anchor)
    }

    private fun decode(data: ByteArray, output: CompressionOutput) {
        val input = CompressionInput(data)
        val expectedSize = input.readVarInt()
        output.checkSize(expectedSize)
        while (input.remaining > 0) {
            val tag = input.readByte()
            when (tag and 3) {
                0 -> {
                    var value = tag ushr 2
                    if (value >= 60) {
                        val byteCount = value - 59
                        value = 0
                        repeat(byteCount) { index -> value = value or (input.readByte() shl (index * 8)) }
                        if (value < 0 || value == Int.MAX_VALUE) throw CompressionInvalidDataException("Snappy literal length overflow", offset = input.position)
                    }
                    val length = value + 1
                    if (length > expectedSize - output.size) throw CompressionInvalidDataException("Snappy literal exceeds declared size", offset = input.position)
                    input.copyTo(output, length)
                }
                else -> {
                    val length: Int
                    val distance: Int
                    when (tag and 3) {
                        1 -> {
                            length = 4 + ((tag ushr 2) and 7)
                            distance = ((tag and 0xE0) shl 3) or input.readByte()
                        }
                        2 -> {
                            length = 1 + (tag ushr 2)
                            distance = input.readShortLE()
                        }
                        else -> {
                            length = 1 + (tag ushr 2)
                            distance = input.readIntLE()
                        }
                    }
                    if (length > expectedSize - output.size) throw CompressionInvalidDataException("Snappy match exceeds declared size", offset = input.position)
                    output.copyMatch(distance, length)
                }
            }
        }
        if (output.size != expectedSize) throw CompressionInvalidDataException("Snappy size does not match its header", offset = input.position)
    }

    override fun decompress(data: ByteArray): ByteArray = CompressionOutput.decompressBytes(maxDecompressedSize) {
        decode(data, it)
    }

    override fun decompressPlaceable(src: ByteArray, dst: ByteArray, dstOffset: Int): Int = CompressionOutput.writeInto(src, dst, dstOffset, maxDecompressedSize) {
        decode(src, it)
    }
}
