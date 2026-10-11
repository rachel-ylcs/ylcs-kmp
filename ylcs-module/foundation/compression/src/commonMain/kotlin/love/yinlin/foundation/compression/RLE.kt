package love.yinlin.foundation.compression

/** PackBits 字节游程编码。适合掩码、索引图和连续重复字节；不包含长度或校验头。 */
class RLE(val maxDecompressedSize: Int = CompressionCodec.DEFAULT_DECOMPRESSION_LIMIT) : CompressionCodec {
    init {
        CompressionCodec.checkDecompressionLimit(maxDecompressedSize)
    }

    override fun maxCompressedSize(sourceSize: Int): Int = CompressionCodec.compressedBound(sourceSize, (sourceSize.toLong() + 127) / 128)

    override fun compress(data: ByteArray): ByteArray  {
        val output = ByteArray(maxCompressedSize(data.size))
        val size = compressPlaceable(data, output)
        return if (size == output.size) output else output.copyOf(size)
    }

    override fun compressPlaceable(src: ByteArray, dst: ByteArray, dstOffset: Int): Int = CompressionOutput.writeInto(src, dst, dstOffset) { output ->
        var position = 0
        while (position < src.size) {
            val begin = position
            val value = src[position]
            var end = position + 1
            val runEnd = minOf(src.size.toLong(), position.toLong() + 128).toInt()
            while (end < runEnd && src[end] == value) end++
            if (end - position >= 3) {
                output.writeByte(257 - (end - position))
                output.writeByte(value.toInt())
                position = end
            }
            else {
                position = end
                while (position < runEnd) {
                    if (position <= src.size - 3 && src[position] == src[position + 1] && src[position] == src[position + 2]) break
                    position++
                }
                output.writeByte(position - begin - 1)
                output.write(src, begin, position - begin)
            }
        }
    }

    private fun decode(data: ByteArray, output: CompressionOutput) {
        val input = CompressionInput(data)
        while (input.remaining > 0) {
            val control = input.readByte()
            when {
                control < 128 -> input.copyTo(output, control + 1)
                control > 128 -> output.fill(input.readByte(), 257 - control)
                // 0x80 是 PackBits 的空操作。
            }
        }
    }

    override fun decompress(data: ByteArray): ByteArray = CompressionOutput.decompressBytes(maxDecompressedSize) {
        decode(data, it)
    }

    override fun decompressPlaceable(src: ByteArray, dst: ByteArray, dstOffset: Int): Int = CompressionOutput.writeInto(src, dst, dstOffset, maxDecompressedSize) {
        decode(src, it)
    }
}
