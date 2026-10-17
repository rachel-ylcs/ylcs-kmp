package love.yinlin.compose.graphics.qrcode

import androidx.compose.runtime.Stable
import love.yinlin.encoding.convert
import love.yinlin.io.ByteArrayIO
import kotlin.math.abs
import kotlin.math.max

@Stable
internal object QrcodeEncoder {
    private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"

    private val ECCPerBlock: Array<IntArray> = [
        [7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28, 28, 28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30],
        [10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28],
        [13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26, 30, 28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30],
        [17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26, 28, 30, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30],
    ]
    private val BlockCounts: Array<IntArray> = [
        [1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 8, 8, 9, 9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25],
        [1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16, 17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49],
        [1, 1, 2, 2, 4, 4, 6, 6, 8, 8, 8, 10, 12, 16, 12, 17, 16, 18, 21, 20, 23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68],
        [1, 1, 2, 4, 4, 4, 5, 6, 8, 8, 11, 11, 16, 16, 18, 16, 19, 21, 25, 25, 25, 34, 30, 32, 35, 37, 40, 42, 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81],
    ]

    fun encode(text: String, level: QrcodeECLevel): QrcodeMatrix {
        require(text.length <= 7089) { "QR code text exceeds the maximum capacity" }

        val mode = when {
            text.isNotEmpty() && text.all { it in '0' .. '9' } -> 1
            text.isNotEmpty() && text.all { it in ALPHABET } -> 2
            else -> 4
        }
        val bytes: ByteArray = if (mode == 4) text.convert() else []
        val length = if (mode == 4) bytes.size else text.length
        val payloadBits = when (mode) {
            1 -> length / 3 * 10 + if (length % 3 == 0) 0 else length % 3 * 3 + 1
            2 -> length / 2 * 11 + length % 2 * 6
            else -> length * 8
        }
        val version = (1 .. 40).firstOrNull {
            4 + lengthBits(mode, it) + payloadBits <= dataCapacity(it, level) * 8
        } ?: throw IllegalArgumentException("QR code text exceeds the capacity of $level")

        val buffer = BitBuffer(dataCapacity(version, level))
        buffer.put(mode, 4)
        buffer.put(length, lengthBits(mode, version))
        when (mode) {
            1 -> {
                var index = 0
                while (index < length) {
                    val count = minOf(3, length - index)
                    var value = 0
                    repeat(count) { value = value * 10 + (text[index++] - '0') }
                    buffer.put(value, count * 3 + 1)
                }
            }
            2 -> {
                var index = 0
                while (index + 1 < length) {
                    buffer.put(ALPHABET.indexOf(text[index]) * 45 + ALPHABET.indexOf(text[index + 1]), 11)
                    index += 2
                }
                if (index < length) buffer.put(ALPHABET.indexOf(text[index]), 6)
            }
            else -> for (byte in bytes) buffer.put(byte.toInt() and 0xff, 8)
        }

        buffer.put(0, minOf(4, buffer.bytes.size * 8 - buffer.size))
        buffer.put(0, (8 - buffer.size % 8) % 8)
        var padding = 0xec
        while (buffer.size < buffer.bytes.size * 8) {
            buffer.put(padding, 8)
            padding = padding xor 0xfd
        }

        return createMatrix(version, level, interleave(buffer.io, version, level))
    }

    private fun lengthBits(mode: Int, version: Int): Int = when (mode) {
        1 -> if (version < 10) 10 else if (version < 27) 12 else 14
        2 -> if (version < 10) 9 else if (version < 27) 11 else 13
        else -> if (version < 10) 8 else 16
    }

    private fun totalCodewords(version: Int): Int {
        var modules = (16 * version + 128) * version + 64
        if (version >= 2) {
            val count = version / 7 + 2
            modules -= (25 * count - 10) * count - 55
            if (version >= 7) modules -= 36
        }
        return modules / 8
    }

    private fun dataCapacity(version: Int, level: QrcodeECLevel): Int = totalCodewords(version) - ECCPerBlock[level.ordinal][version - 1] * BlockCounts[level.ordinal][version - 1]

    private fun interleave(data: ByteArrayIO, version: Int, level: QrcodeECLevel): IntArray {
        val blockCount = BlockCounts[level.ordinal][version - 1]
        val eccCount = ECCPerBlock[level.ordinal][version - 1]
        val total = totalCodewords(version)
        val shortDataCount = total / blockCount - eccCount
        val shortBlockCount = blockCount - total % blockCount
        val divisor = IntArray(eccCount).apply { this[lastIndex] = 1 }
        var root = 1
        repeat(eccCount) {
            for (i in divisor.indices) {
                divisor[i] = multiply(divisor[i], root)
                if (i < divisor.lastIndex) divisor[i] = divisor[i] xor divisor[i + 1]
            }
            root = multiply(root, 2)
        }

        var offset = 0
        val blocks = Array(blockCount) { index ->
            val count = shortDataCount + if (index < shortBlockCount) 0 else 1
            IntArray(count) { data.readUByte(offset++).toInt() }
        }
        val corrections = Array(blockCount) { index ->
            val remainder = IntArray(eccCount)
            for (byte in blocks[index]) {
                val factor = byte xor remainder[0]
                for (i in 0 ..< eccCount - 1) {
                    remainder[i] = remainder[i + 1] xor multiply(divisor[i], factor)
                }
                remainder[eccCount - 1] = multiply(divisor[eccCount - 1], factor)
            }
            remainder
        }

        val result = IntArray(total)
        var index = 0
        for (i in 0 .. shortDataCount) {
            for (block in blocks) {
                if (i < block.size) result[index++] = block[i]
            }
        }
        for (i in 0 ..< eccCount) {
            for (correction in corrections) result[index++] = correction[i]
        }
        return result
    }

    private fun multiply(left: Int, right: Int): Int {
        var a = left
        var b = right
        var result = 0
        repeat(8) {
            if (b and 1 != 0) result = result xor a
            b = b ushr 1
            a = a shl 1
            if (a and 0x100 != 0) a = a xor 0x11d
        }
        return result
    }

    private fun createMatrix(version: Int, level: QrcodeECLevel, data: IntArray): QrcodeMatrix {
        val size = version * 4 + 17
        val modules = ByteArray(size * size) { -1 }
        fun set(x: Int, y: Int, dark: Boolean) {
            modules[y * size + x] = if (dark) 1 else 0
        }
        fun finder(left: Int, top: Int) {
            for (y in -1 .. 7) {
                for (x in -1 .. 7) {
                    if (left + x in 0 ..< size && top + y in 0 ..< size) {
                        set(left + x, top + y, x in 0 .. 6 && y in 0 .. 6 &&
                            (x == 0 || x == 6 || y == 0 || y == 6 || x in 2 .. 4 && y in 2 .. 4))
                    }
                }
            }
        }
        finder(0, 0)
        finder(size - 7, 0)
        finder(0, size - 7)

        if (version >= 2) {
            val count = version / 7 + 2
            val step = if (version == 32) 26 else (version * 4 + count * 2 + 1) / (count * 2 - 2) * 2
            val positions = IntArray(count) { if (it == 0) 6 else size - 7 - (count - 1 - it) * step }
            for (y in positions) {
                for (x in positions) {
                    if (modules[y * size + x] < 0) {
                        for (dy in -2 .. 2) {
                            for (dx in -2 .. 2) set(x + dx, y + dy, max(abs(dx), abs(dy)) != 1)
                        }
                    }
                }
            }
        }
        for (i in 8 ..< size - 8) {
            if (modules[i * size + 6] < 0) set(6, i, i % 2 == 0)
            if (modules[6 * size + i] < 0) set(i, 6, i % 2 == 0)
        }

        val formatData = level.formatBits shl 3
        var remainder = formatData
        repeat(10) { remainder = (remainder shl 1) xor ((remainder ushr 9) * 0x537) }
        val format = ((formatData shl 10) or remainder) xor 0x5412
        for (i in 0 ..< 15) {
            val dark = format ushr i and 1 != 0
            set(8, if (i < 6) i else if (i < 8) i + 1 else size - 15 + i, dark)
            set(if (i < 8) size - i - 1 else if (i == 8) 7 else 14 - i, 8, dark)
        }
        set(8, size - 8, true)

        if (version >= 7) {
            remainder = version
            repeat(12) { remainder = (remainder shl 1) xor ((remainder ushr 11) * 0x1f25) }
            val bits = (version shl 12) or remainder
            for (i in 0 ..< 18) {
                val a = size - 11 + i % 3
                val b = i / 3
                val dark = bits ushr i and 1 != 0
                set(a, b, dark)
                set(b, a, dark)
            }
        }

        val functions = BooleanArray(modules.size) { modules[it] >= 0 }
        var bit = 0
        var right = size - 1
        var upwards = true
        while (right > 0) {
            if (right == 6) right--
            for (i in 0 ..< size) {
                val y = if (upwards) size - 1 - i else i
                for (dx in 0 .. 1) {
                    val x = right - dx
                    if (modules[y * size + x] < 0) {
                        val dark = bit < data.size * 8 && data[bit / 8] ushr (7 - bit % 8) and 1 != 0
                        set(x, y, dark xor ((x + y) % 2 == 0))
                        bit++
                    }
                }
            }
            upwards = !upwards
            right -= 2
        }
        return QrcodeMatrix(size, BooleanArray(modules.size) { modules[it] == 1.toByte() }, functions)
    }

    private class BitBuffer(capacity: Int) {
        val io = ByteArrayIO(capacity)
        val bytes: ByteArray get() = io.bytes
        var size = 0
            private set

        fun put(value: Int, count: Int) {
            var remaining = count
            while (remaining > 0) {
                val free = 8 - size % 8
                val bits = minOf(free, remaining)
                val part = (value ushr (remaining - bits) and ((1 shl bits) - 1)) shl (free - bits)
                val index = size / 8
                io.writeByte(index, (io.readUByte(index).toInt() or part).toByte())
                size += bits
                remaining -= bits
            }
        }
    }
}
