package love.yinlin.foundation.cryptography

import love.yinlin.extension.cleaning
import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian
import kotlin.collections.listOf

class XChaCha20(private val key: ByteArray) : Cipher() {
    constructor(key: String) : this(key.encodeToByteArray())

    private object ChaCha20 {
        val Constants: IntArray = [0x61707865, 0x3320646e, 0x79622d32, 0x6b206574]
        val Destinations = [0, 1, 2, 3, 12, 13, 14, 15].withIndex()

        fun quarter(state: IntArray, a: Int, b: Int, c: Int, d: Int) {
            state[a] += state[b]
            state[d] = (state[d] xor state[a]).rotateLeft(16)
            state[c] += state[d]
            state[b] = (state[b] xor state[c]).rotateLeft(12)
            state[a] += state[b]
            state[d] = (state[d] xor state[a]).rotateLeft(8)
            state[c] += state[d]
            state[b] = (state[b] xor state[c]).rotateLeft(7)
        }

        fun permute(state: IntArray) {
            repeat(10) {
                quarter(state, 0, 4, 8, 12)
                quarter(state, 1, 5, 9, 13)
                quarter(state, 2, 6, 10, 14)
                quarter(state, 3, 7, 11, 15)
                quarter(state, 0, 5, 10, 15)
                quarter(state, 1, 6, 11, 12)
                quarter(state, 2, 7, 8, 13)
                quarter(state, 3, 4, 9, 14)
            }
        }

        fun hChaCha20(key: ByteArray, nonce: ByteArray): ByteArray {
            require(key.size == 32 && nonce.size == 16)
            val working = IntArray(16)
            Constants.copyInto(working)
            val keyIO = ByteArrayIO(key)
            val nonceIO = ByteArrayIO(nonce)
            for (index in 0..7) working[4 + index] = keyIO.readInt(index * 4, Endian.LITTLE)
            for (index in 0..3) working[12 + index] = nonceIO.readInt(index * 4, Endian.LITTLE)
            permute(working)
            return ByteArrayIO(32).write {
                for ([destination, source] in Destinations) writeInt(destination * 4, working[source], Endian.LITTLE)
                working.fill(0)
            }
        }

        fun block(key: ByteArray, nonce: ByteArray, counter: UInt = 0u): ByteArray {
            require(key.size == 32 && nonce.size == 12)
            val initial = IntArray(16)
            Constants.copyInto(initial)
            val keyIO = ByteArrayIO(key)
            val nonceIO = ByteArrayIO(nonce)
            for (index in 0..7) initial[4 + index] = keyIO.readInt(index * 4, Endian.LITTLE)
            initial[12] = counter.toInt()
            for (index in 0..2) initial[13 + index] = nonceIO.readInt(index * 4, Endian.LITTLE)
            val working = initial.copyOf()
            permute(working)
            return ByteArrayIO(64).write {
                for (index in working.indices) writeInt(index * 4, working[index] + initial[index], Endian.LITTLE)
                initial.fill(0)
                working.fill(0)
            }
        }

        fun transform(key: ByteArray, nonce: ByteArray, data: ByteArray, counter: UInt = 0u, byteOffset: Int = 0): ByteArray {
            require(key.size == 32 && nonce.size == 12)
            require(byteOffset in 0..63)
            val blocks = (byteOffset.toLong() + data.size + 63) / 64
            require(blocks == 0L || counter.toLong() + blocks - 1 <= UInt.MAX_VALUE.toLong()) { "ChaCha20 counter overflow" }
            val output = ByteArray(data.size)
            var cursor = 0
            var blockCounter = counter
            var skip = byteOffset
            while (cursor < data.size) {
                val block = block(key, nonce, blockCounter)
                val count = minOf(64 - skip, data.size - cursor)
                for (index in 0 until count) output[cursor + index] = (data[cursor + index].toInt() xor block[skip + index].toInt()).toByte()
                block.fill(0)
                cursor += count
                blockCounter++
                skip = 0
            }
            return output
        }
    }

    private object XChaCha20 {
        fun transform(key: ByteArray, nonce: ByteArray, data: ByteArray, counter: UInt = 0u, byteOffset: Int = 0): ByteArray {
            require(key.size == 32 && nonce.size == 24)
            val subkey = ChaCha20.hChaCha20(key, nonce.copyOfRange(0, 16))
            val shortNonce = ByteArray(12)
            nonce.copyInto(shortNonce, 4, 16, 24)
            return cleaning({ subkey.fill(0) }) { ChaCha20.transform(subkey, shortNonce, data, counter, byteOffset) }
        }
    }

    init {
        require(key.size == 32) { "XChaCha20 key size must be 32 bytes." }
    }

    override fun encode(data: ByteArray): ByteArray {
        val nonce = SecureEntropy.bytes(24)
        require(data.size <= Int.MAX_VALUE - nonce.size)
        return nonce + XChaCha20.transform(key, nonce, data)
    }

    override fun decode(data: ByteArray): ByteArray {
        require(data.size >= 24) { "Truncated XChaCha20 packet." }
        return XChaCha20.transform(key, data.copyOfRange(0, 24), data.copyOfRange(24, data.size))
    }
}