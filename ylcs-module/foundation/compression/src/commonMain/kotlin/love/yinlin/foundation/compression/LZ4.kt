package love.yinlin.foundation.compression

import love.yinlin.extension.forever
import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

/** LZ4 原始块格式（非 LZ4 Frame）。无长度头或校验和，优先用于高速压缩。 */
class LZ4(val maxDecompressedSize: Int = CompressionCodec.DEFAULT_DECOMPRESSION_LIMIT) : CompressionCodec {
    init {
        CompressionCodec.checkDecompressionLimit(maxDecompressedSize)
    }

    override fun maxCompressedSize(sourceSize: Int): Int = CompressionCodec.compressedBound(sourceSize, sourceSize.toLong() / 255 + 16)

    private fun writeLength(output: CompressionOutput, length: Int) {
        var remaining = length
        while (remaining >= 255) {
            output.writeByte(255)
            remaining -= 255
        }
        output.writeByte(remaining)
    }

    override fun compress(data: ByteArray): ByteArray  {
        val output = ByteArray(maxCompressedSize(data.size))
        val size = compressPlaceable(data, output)
        return if (size == output.size) output else output.copyOf(size)
    }

    override fun compressPlaceable(src: ByteArray, dst: ByteArray, dstOffset: Int): Int = CompressionOutput.writeInto(src, dst, dstOffset) { output ->
        val io = ByteArrayIO(src)
        var anchor = 0
        var position = 0
        // LZ4 要求最后 5 个字节是字面量，最后一个匹配至少从倒数第 12 个字节开始。
        val matchLimit = src.size - 5
        val searchLimit = src.size - 12
        if (searchLimit >= 0) {
            val table = IntArray(65536) { -1 }
            while (position <= searchLimit) {
                val hash = (io.readInt(position, Endian.LITTLE) * -1640531535) ushr 16
                val candidate = table[hash]
                table[hash] = position
                if (candidate < 0 || position - candidate > 65535 || io.readInt(candidate, Endian.LITTLE) != io.readInt(position, Endian.LITTLE)) {
                    position++
                    continue
                }

                var length = 4
                while (length < matchLimit - position && src[candidate + length] == src[position + length]) length++
                val literalLength = position - anchor
                output.writeByte((minOf(literalLength, 15) shl 4) or minOf(length - 4, 15))
                if (literalLength >= 15) writeLength(output, literalLength - 15)
                output.write(src, anchor, literalLength)
                output.writeShortLE(position - candidate)
                if (length >= 19) writeLength(output, length - 19)

                position += length
                anchor = position
                // 更新匹配尾部即可，避免为长匹配逐字节重建哈希表。
                val p2 = (io.readInt(position - 2, Endian.LITTLE) * -1640531535) ushr 16
                val p1 = (io.readInt(position - 1, Endian.LITTLE) * -1640531535) ushr 16
                table[p2] = position - 2
                table[p1] = position - 1
            }
        }
        val literalLength = src.size - anchor
        output.writeByte(minOf(literalLength, 15) shl 4)
        if (literalLength >= 15) writeLength(output, literalLength - 15)
        output.write(src, anchor, literalLength)
    }

    private fun readLength(input: CompressionInput, initial: Int): Int {
        var length = initial
        if (initial == 15) {
            do {
                val extra = input.readByte()
                if (length > Int.MAX_VALUE - extra) throw CompressionInvalidDataException("LZ4 length overflow", offset = input.position - 1)
                length += extra
            } while (extra == 255)
        }
        return length
    }

    private fun decode(data: ByteArray, output: CompressionOutput) {
        val input = CompressionInput(data)
        if (input.remaining == 0) throw CompressionTruncatedInputException("Missing LZ4 token", 0)
        forever {
            val token = input.readByte()
            val literalLength = readLength(input, token ushr 4)
            input.copyTo(output, literalLength)
            if (input.remaining == 0) return
            val distance = input.readShortLE()
            val length = readLength(input, token and 15)
            if (length > Int.MAX_VALUE - 4) throw CompressionInvalidDataException("LZ4 match length overflow", offset = input.position)
            output.copyMatch(distance, length + 4)
            if (input.remaining == 0) throw CompressionTruncatedInputException("Missing final LZ4 literal sequence", input.position)
        }
    }

    override fun decompress(data: ByteArray): ByteArray = CompressionOutput.decompressBytes(maxDecompressedSize) {
        decode(data, it)
    }

    override fun decompressPlaceable(src: ByteArray, dst: ByteArray, dstOffset: Int): Int = CompressionOutput.writeInto(src, dst, dstOffset, maxDecompressedSize) {
        decode(src, it)
    }
}
