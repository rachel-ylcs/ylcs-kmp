package love.yinlin.foundation.compression

import love.yinlin.io.Adler32

/** RFC 1950 Zlib：Deflate + Adler-32。支持标准窗口大小，不支持预置字典。 */
class Zlib(val maxDecompressedSize: Int = CompressionCodec.DEFAULT_DECOMPRESSION_LIMIT) : CompressionCodec {
    private val deflate = Deflate(maxDecompressedSize)

    override fun maxCompressedSize(sourceSize: Int): Int = CompressionCodec.compressedBound(deflate.maxCompressedSize(sourceSize), 6)

    override fun compress(data: ByteArray): ByteArray  {
        val output = ByteArray(maxCompressedSize(data.size))
        val size = compressPlaceable(data, output)
        return if (size == output.size) output else output.copyOf(size)
    }

    override fun compressPlaceable(src: ByteArray, dst: ByteArray, dstOffset: Int): Int = CompressionOutput.writeInto(src, dst, dstOffset) { output ->
        output.writeByte(0x78)
        output.writeByte(0x01)
        deflate.encode(src, output)
        output.writeIntBE(Adler32.calculate(src))
    }

    private fun decode(data: ByteArray, output: CompressionOutput) {
        val input = CompressionInput(data)
        val method = input.readByte()
        val flags = input.readByte()
        if (method and 15 != 8 || method ushr 4 > 7) throw CompressionUnsupportedFormatException("Unsupported Zlib compression method", 0)
        if (((method shl 8) or flags) % 31 != 0) throw CompressionInvalidDataException("Invalid Zlib header check", offset = 1)
        if (flags and 0x20 != 0) throw CompressionUnsupportedFormatException("Zlib preset dictionaries are not supported", 1)
        val begin = output.position
        input.position = deflate.decode(data, output, input.position, windowSize = 1 shl ((method ushr 4) + 8))
        val expected = input.readIntBE()
        if (expected != Adler32.calculate(output.bytes, begin, output.position)) throw CompressionChecksumMismatchException("Zlib Adler-32 mismatch", input.position - 4)
        if (input.remaining != 0) throw CompressionInvalidDataException("Trailing data after Zlib stream", offset = input.position)
    }

    override fun decompress(data: ByteArray): ByteArray = CompressionOutput.decompressBytes(maxDecompressedSize) {
        decode(data, it)
    }

    override fun decompressPlaceable(src: ByteArray, dst: ByteArray, dstOffset: Int): Int = CompressionOutput.writeInto(src, dst, dstOffset, maxDecompressedSize) {
        decode(src, it)
    }
}
