package love.yinlin.foundation.compression

import love.yinlin.extension.loop
import love.yinlin.io.CRC32

/** RFC 1952 Gzip：支持可选头字段、头部 CRC16、CRC32/ISIZE 和串联成员。 */
class Gzip(val maxDecompressedSize: Int = CompressionCodec.DEFAULT_DECOMPRESSION_LIMIT) : CompressionCodec {
    private val deflate = Deflate(maxDecompressedSize)

    override fun maxCompressedSize(sourceSize: Int): Int = CompressionCodec.compressedBound(deflate.maxCompressedSize(sourceSize), 18)

    override fun compress(data: ByteArray): ByteArray  {
        val output = ByteArray(maxCompressedSize(data.size))
        val size = compressPlaceable(data, output)
        return if (size == output.size) output else output.copyOf(size)
    }

    override fun compressPlaceable(src: ByteArray, dst: ByteArray, dstOffset: Int): Int = CompressionOutput.writeInto(src, dst, dstOffset) { output ->
        output.writeShortLE(0x8B1F)
        output.writeByte(8)
        output.writeByte(0)
        output.writeIntLE(0) // MTIME 固定为 0，输出可复现。
        output.writeByte(0)
        output.writeByte(255) // 未指定 OS。
        deflate.encode(src, output)
        output.writeIntLE(CRC32.calculate(src))
        output.writeIntLE(src.size)
    }

    private fun skipString(input: CompressionInput) {
        loop { input.readByte() != 0 }
    }

    private fun decode(data: ByteArray, output: CompressionOutput) {
        val input = CompressionInput(data)
        val crc32 = CRC32()
        do {
            val headerStart = input.position
            if (input.readShortLE() != 0x8B1F) throw CompressionInvalidDataException("Invalid Gzip signature", offset = headerStart)
            if (input.readByte() != 8) throw CompressionUnsupportedFormatException("Unsupported Gzip compression method", headerStart + 2)
            val flags = input.readByte()
            if (flags and 0xE0 != 0) throw CompressionInvalidDataException("Reserved Gzip flags", offset = headerStart + 3)
            input.skip(6)
            if (flags and 4 != 0) input.skip(input.readShortLE())
            if (flags and 8 != 0) skipString(input)
            if (flags and 16 != 0) skipString(input)
            if (flags and 2 != 0) {
                val crc = crc32.update(data, headerStart, input.position).reset() and 0xFFFF
                if (input.readShortLE() != crc) throw CompressionChecksumMismatchException("Gzip header CRC16 mismatch", input.position - 2)
            }
            val begin = output.position
            input.position = deflate.decode(data, output, input.position)
            val crc = input.readIntLE()
            val size = input.readIntLE()
            if (crc != crc32.update(output.bytes, begin, output.position).reset()) {
                throw CompressionChecksumMismatchException("Gzip CRC32 mismatch", input.position - 8)
            }
            if (size != output.position - begin) throw CompressionInvalidDataException("Gzip ISIZE mismatch", offset = input.position - 4)
        } while (input.remaining > 0)
    }

    override fun decompress(data: ByteArray): ByteArray = CompressionOutput.decompressBytes(maxDecompressedSize) {
        decode(data, it)
    }

    override fun decompressPlaceable(src: ByteArray, dst: ByteArray, dstOffset: Int): Int = CompressionOutput.writeInto(src, dst, dstOffset, maxDecompressedSize) {
        decode(src, it)
    }
}
