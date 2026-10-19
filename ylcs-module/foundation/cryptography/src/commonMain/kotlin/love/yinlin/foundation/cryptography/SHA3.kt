package love.yinlin.foundation.cryptography

import love.yinlin.extension.cleaning
import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

class SHA3(type: BitType = BitType.B256) : ByteDigest() {
    enum class BitType(val bits: Int) {
        B224(224),
        B256(256),
        B384(384),
        B512(512);
    }

    private companion object {
        val Rotations: IntArray = [0, 1, 62, 28, 27, 36, 44, 6, 55, 20, 3, 10, 43, 25, 39, 41, 45, 15, 21, 8, 18, 2, 61, 56, 14]
        val Rounds: LongArray = [
            0x0000000000000001uL.toLong(), 0x0000000000008082uL.toLong(),
            0x800000000000808auL.toLong(), 0x8000000080008000uL.toLong(),
            0x000000000000808buL.toLong(), 0x0000000080000001uL.toLong(),
            0x8000000080008081uL.toLong(), 0x8000000000008009uL.toLong(),
            0x000000000000008auL.toLong(), 0x0000000000000088uL.toLong(),
            0x0000000080008009uL.toLong(), 0x000000008000000auL.toLong(),
            0x000000008000808buL.toLong(), 0x800000000000008buL.toLong(),
            0x8000000000008089uL.toLong(), 0x8000000000008003uL.toLong(),
            0x8000000000008002uL.toLong(), 0x8000000000000080uL.toLong(),
            0x000000000000800auL.toLong(), 0x800000008000000auL.toLong(),
            0x8000000080008081uL.toLong(), 0x8000000000008080uL.toLong(),
            0x0000000080000001uL.toLong(), 0x8000000080008008uL.toLong(),
        ]
    }

    private class Sha3Stream(private val type: BitType) {
        val rate: Int = (1600 - type.bits * 2) / 8
        val state = LongArray(25)
        val buffer: ByteArray = ByteArray(rate)
        private val columns = LongArray(5)
        private val theta = LongArray(5)
        private val rotated = LongArray(25)
        var buffered = 0

        fun update(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): Sha3Stream {
            require(offset >= 0 && length >= 0 && offset <= data.size - length)
            var cursor = offset
            val end = offset + length
            while (cursor < end) {
                if (buffered == 0 && end - cursor >= rate) {
                    absorb(data, cursor)
                    cursor += rate
                    continue
                }

                val count = minOf(rate - buffered, end - cursor)
                data.copyInto(buffer, buffered, cursor, cursor + count)
                buffered += count
                cursor += count
                if (buffered == rate) {
                    absorb(buffer)
                    buffered = 0
                }
            }
            return this
        }

        fun finish(): ByteArray {
            buffer.fill(0, buffered, rate)
            buffer[buffered] = 0x06
            buffer[rate - 1] = (buffer[rate - 1].toInt() xor 0x80).toByte()
            absorb(buffer)
            return ByteArray(type.bits / 8) { index -> (state[index / 8] ushr ((index % 8) * 8)).toByte() }
        }

        private fun absorb(block: ByteArray, offset: Int = 0) {
            val blockIO = ByteArrayIO(block)
            for (index in 0 ..< rate / 8) {
                state[index] = state[index] xor blockIO.readLong(offset + index * 8, Endian.LITTLE)
            }
            for (constant in Rounds) {
                for (x in 0 .. 4) columns[x] = state[x] xor state[x + 5] xor state[x + 10] xor state[x + 15] xor state[x + 20]
                for (x in 0 .. 4) theta[x] = columns[(x + 4) % 5] xor columns[(x + 1) % 5].rotateLeft(1)
                for (y in 0 .. 4) for (x in 0 .. 4) state[x + 5 * y] = state[x + 5 * y] xor theta[x]
                for (y in 0 .. 4) for (x in 0 .. 4) {
                    rotated[y + 5 * ((2 * x + 3 * y) % 5)] = state[x + 5 * y].rotateLeft(Rotations[x + 5 * y])
                }
                for (y in 0 .. 4) {
                    for (x in 0 .. 4) {
                        state[x + 5 * y] = rotated[x + 5 * y] xor (rotated[(x + 1) % 5 + 5 * y].inv() and rotated[(x + 2) % 5 + 5 * y])
                    }
                }
                state[0] = state[0] xor constant
            }
        }

        fun reset(): Sha3Stream {
            state.fill(0)
            buffer.fill(0)
            columns.fill(0)
            theta.fill(0)
            rotated.fill(0)
            buffered = 0
            return this
        }
    }

    private val stream = Sha3Stream(type)

    override fun encode(data: ByteArray): ByteArray = cleaning({
        stream.update(data).finish()
    }, stream::reset)
}