package love.yinlin.foundation.compression

import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

internal class CompressionOutput private constructor(
    bytes: ByteArray,
    val start: Int,
    private val limit: Int,
    private val growable: Boolean,
) {
    constructor(limit: Int) : this(ByteArray(minOf(256, limit)), 0, limit, true)
    constructor(bytes: ByteArray, offset: Int, limit: Int) : this(bytes, offset, limit, false)

    private var io = ByteArrayIO(bytes)
    val bytes: ByteArray get() = io.bytes
    var position: Int = start
        private set
    val size: Int get() = position - start

    fun checkSize(expected: Int) {
        if (expected !in 0 .. limit) throw CompressionOutputLimitExceededException("Decompressed data exceeds the configured limit")
        if (!growable && expected > bytes.size - start) throw CompressionOutputTooSmallException("Destination buffer is too small")
    }

    private fun ensure(count: Int) {
        if (count < 0 || count > limit - size) throw CompressionOutputLimitExceededException("Output exceeds the configured limit")
        if (count > bytes.size - position) {
            if (!growable) throw CompressionOutputTooSmallException("Destination buffer is too small")
            val capacity = maxOf(position.toLong() + count, maxOf(256L, bytes.size.toLong() * 2)).coerceAtMost(limit.toLong()).toInt()
            io = ByteArrayIO(bytes.copyOf(capacity))
        }
    }

    fun writeByte(value: Int) {
        ensure(1)
        io.writeByte(position++, value.toByte())
    }

    fun writeShortLE(value: Int) {
        ensure(2)
        io.writeShort(position, value.toShort(), Endian.LITTLE)
        position += 2
    }

    fun writeIntLE(value: Int) {
        ensure(4)
        io.writeInt(position, value, Endian.LITTLE)
        position += 4
    }

    fun writeIntBE(value: Int) {
        ensure(4)
        io.writeInt(position, value, Endian.BIG)
        position += 4
    }

    fun write(data: ByteArray, offset: Int, count: Int) {
        ensure(count)
        data.copyInto(bytes, position, offset, offset + count)
        position += count
    }

    fun fill(value: Int, count: Int) {
        ensure(count)
        bytes.fill(value.toByte(), position, position + count)
        position += count
    }

    // 重叠匹配按已生成的前缀倍增复制，避免逐字节处理长重复串。
    fun copyMatch(distance: Int, count: Int, historyStart: Int = 0) {
        if (distance <= 0 || distance > size - historyStart) throw CompressionInvalidDataException("Invalid match distance")
        ensure(count)
        if (distance == 1) bytes.fill(bytes[position - 1], position, position + count)
        else {
            var copied = minOf(distance, count)
            bytes.copyInto(bytes, position, position - distance, position - distance + copied)
            while (copied < count) {
                val length = minOf(copied, count - copied)
                bytes.copyInto(bytes, position + copied, position, position + length)
                copied += length
            }
        }
        position += count
    }

    fun writeVarInt(value: Int) {
        var remaining = value
        while (remaining >= 0x80) {
            writeByte((remaining and 0x7F) or 0x80)
            remaining = remaining ushr 7
        }
        writeByte(remaining)
    }

    fun toByteArray(): ByteArray = if (start == 0 && position == bytes.size) bytes else bytes.copyOfRange(start, position)

    companion object {
        internal inline fun decompressBytes(limit: Int, block: (CompressionOutput) -> Unit): ByteArray {
            val output = CompressionOutput(limit)
            block(output)
            return output.toByteArray()
        }

        internal inline fun writeInto(src: ByteArray, dst: ByteArray, offset: Int, limit: Int = Int.MAX_VALUE, block: (CompressionOutput) -> Unit): Int {
            if (src === dst) throw CompressionInvalidArgumentException("Source and destination must be different arrays")
            if (offset < 0 || offset > dst.size) throw CompressionInvalidArgumentException("Invalid destination offset")
            val output = CompressionOutput(dst, offset, limit)
            block(output)
            return output.size
        }
    }
}