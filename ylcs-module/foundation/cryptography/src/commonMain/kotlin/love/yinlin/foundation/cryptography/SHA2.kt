package love.yinlin.foundation.cryptography

import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

class SHA2(type: BitType = BitType.B256) : ByteDigest() {
    enum class BitType(val bits: Int) {
        B224(224),
        B256(256),
        B384(384),
        B512(512);
    }

    private companion object {
        val K256: IntArray = [
            0x428a2f98u.toInt(), 0x71374491u.toInt(), 0xb5c0fbcfu.toInt(), 0xe9b5dba5u.toInt(),
            0x3956c25bu.toInt(), 0x59f111f1u.toInt(), 0x923f82a4u.toInt(), 0xab1c5ed5u.toInt(),
            0xd807aa98u.toInt(), 0x12835b01u.toInt(), 0x243185beu.toInt(), 0x550c7dc3u.toInt(),
            0x72be5d74u.toInt(), 0x80deb1feu.toInt(), 0x9bdc06a7u.toInt(), 0xc19bf174u.toInt(),
            0xe49b69c1u.toInt(), 0xefbe4786u.toInt(), 0x0fc19dc6u.toInt(), 0x240ca1ccu.toInt(),
            0x2de92c6fu.toInt(), 0x4a7484aau.toInt(), 0x5cb0a9dcu.toInt(), 0x76f988dau.toInt(),
            0x983e5152u.toInt(), 0xa831c66du.toInt(), 0xb00327c8u.toInt(), 0xbf597fc7u.toInt(),
            0xc6e00bf3u.toInt(), 0xd5a79147u.toInt(), 0x06ca6351u.toInt(), 0x14292967u.toInt(),
            0x27b70a85u.toInt(), 0x2e1b2138u.toInt(), 0x4d2c6dfcu.toInt(), 0x53380d13u.toInt(),
            0x650a7354u.toInt(), 0x766a0abbu.toInt(), 0x81c2c92eu.toInt(), 0x92722c85u.toInt(),
            0xa2bfe8a1u.toInt(), 0xa81a664bu.toInt(), 0xc24b8b70u.toInt(), 0xc76c51a3u.toInt(),
            0xd192e819u.toInt(), 0xd6990624u.toInt(), 0xf40e3585u.toInt(), 0x106aa070u.toInt(),
            0x19a4c116u.toInt(), 0x1e376c08u.toInt(), 0x2748774cu.toInt(), 0x34b0bcb5u.toInt(),
            0x391c0cb3u.toInt(), 0x4ed8aa4au.toInt(), 0x5b9cca4fu.toInt(), 0x682e6ff3u.toInt(),
            0x748f82eeu.toInt(), 0x78a5636fu.toInt(), 0x84c87814u.toInt(), 0x8cc70208u.toInt(),
            0x90befffau.toInt(), 0xa4506cebu.toInt(), 0xbef9a3f7u.toInt(), 0xc67178f2u.toInt(),
        ]

        val K512: LongArray = [
            0x428a2f98d728ae22uL.toLong(), 0x7137449123ef65cduL.toLong(),
            0xb5c0fbcfec4d3b2fuL.toLong(), 0xe9b5dba58189dbbcuL.toLong(),
            0x3956c25bf348b538uL.toLong(), 0x59f111f1b605d019uL.toLong(),
            0x923f82a4af194f9buL.toLong(), 0xab1c5ed5da6d8118uL.toLong(),
            0xd807aa98a3030242uL.toLong(), 0x12835b0145706fbeuL.toLong(),
            0x243185be4ee4b28cuL.toLong(), 0x550c7dc3d5ffb4e2uL.toLong(),
            0x72be5d74f27b896fuL.toLong(), 0x80deb1fe3b1696b1uL.toLong(),
            0x9bdc06a725c71235uL.toLong(), 0xc19bf174cf692694uL.toLong(),
            0xe49b69c19ef14ad2uL.toLong(), 0xefbe4786384f25e3uL.toLong(),
            0x0fc19dc68b8cd5b5uL.toLong(), 0x240ca1cc77ac9c65uL.toLong(),
            0x2de92c6f592b0275uL.toLong(), 0x4a7484aa6ea6e483uL.toLong(),
            0x5cb0a9dcbd41fbd4uL.toLong(), 0x76f988da831153b5uL.toLong(),
            0x983e5152ee66dfabuL.toLong(), 0xa831c66d2db43210uL.toLong(),
            0xb00327c898fb213fuL.toLong(), 0xbf597fc7beef0ee4uL.toLong(),
            0xc6e00bf33da88fc2uL.toLong(), 0xd5a79147930aa725uL.toLong(),
            0x06ca6351e003826fuL.toLong(), 0x142929670a0e6e70uL.toLong(),
            0x27b70a8546d22ffcuL.toLong(), 0x2e1b21385c26c926uL.toLong(),
            0x4d2c6dfc5ac42aeduL.toLong(), 0x53380d139d95b3dfuL.toLong(),
            0x650a73548baf63deuL.toLong(), 0x766a0abb3c77b2a8uL.toLong(),
            0x81c2c92e47edaee6uL.toLong(), 0x92722c851482353buL.toLong(),
            0xa2bfe8a14cf10364uL.toLong(), 0xa81a664bbc423001uL.toLong(),
            0xc24b8b70d0f89791uL.toLong(), 0xc76c51a30654be30uL.toLong(),
            0xd192e819d6ef5218uL.toLong(), 0xd69906245565a910uL.toLong(),
            0xf40e35855771202auL.toLong(), 0x106aa07032bbd1b8uL.toLong(),
            0x19a4c116b8d2d0c8uL.toLong(), 0x1e376c085141ab53uL.toLong(),
            0x2748774cdf8eeb99uL.toLong(), 0x34b0bcb5e19b48a8uL.toLong(),
            0x391c0cb3c5c95a63uL.toLong(), 0x4ed8aa4ae3418acbuL.toLong(),
            0x5b9cca4f7763e373uL.toLong(), 0x682e6ff3d6b2b8a3uL.toLong(),
            0x748f82ee5defb2fcuL.toLong(), 0x78a5636f43172f60uL.toLong(),
            0x84c87814a1f0ab72uL.toLong(), 0x8cc702081a6439ecuL.toLong(),
            0x90befffa23631e28uL.toLong(), 0xa4506cebde82bde9uL.toLong(),
            0xbef9a3f7b2c67915uL.toLong(), 0xc67178f2e372532buL.toLong(),
            0xca273eceea26619cuL.toLong(), 0xd186b8c721c0c207uL.toLong(),
            0xeada7dd6cde0eb1euL.toLong(), 0xf57d4f7fee6ed178uL.toLong(),
            0x06f067aa72176fbauL.toLong(), 0x0a637dc5a2c898a6uL.toLong(),
            0x113f9804bef90daeuL.toLong(), 0x1b710b35131c471buL.toLong(),
            0x28db77f523047d84uL.toLong(), 0x32caab7b40c72493uL.toLong(),
            0x3c9ebe0a15c9bebcuL.toLong(), 0x431d67c49c100d4cuL.toLong(),
            0x4cc5d4becb3e42b6uL.toLong(), 0x597f299cfc657e2auL.toLong(),
            0x5fcb6fab3ad6faecuL.toLong(), 0x6c44198c4a475817uL.toLong(),
        ]

        val Initial224: IntArray = [
            0xc1059ed8u.toInt(),
            0x367cd507u.toInt(),
            0x3070dd17u.toInt(),
            0xf70e5939u.toInt(),
            0xffc00b31u.toInt(),
            0x68581511u.toInt(),
            0x64f98fa7u.toInt(),
            0xbefa4fa4u.toInt(),
        ]

        val Initial256: IntArray = [
            0x6a09e667u.toInt(),
            0xbb67ae85u.toInt(),
            0x3c6ef372u.toInt(),
            0xa54ff53au.toInt(),
            0x510e527fu.toInt(),
            0x9b05688cu.toInt(),
            0x1f83d9abu.toInt(),
            0x5be0cd19u.toInt(),
        ]

        val Initial384: LongArray = [
            0xcbbb9d5dc1059ed8uL.toLong(),
            0x629a292a367cd507uL.toLong(),
            0x9159015a3070dd17uL.toLong(),
            0x152fecd8f70e5939uL.toLong(),
            0x67332667ffc00b31uL.toLong(),
            0x8eb44a8768581511uL.toLong(),
            0xdb0c2e0d64f98fa7uL.toLong(),
            0x47b5481dbefa4fa4uL.toLong(),
        ]

        val Initial512: LongArray = [
            0x6a09e667f3bcc908uL.toLong(),
            0xbb67ae8584caa73buL.toLong(),
            0x3c6ef372fe94f82buL.toLong(),
            0xa54ff53a5f1d36f1uL.toLong(),
            0x510e527fade682d1uL.toLong(),
            0x9b05688c2b3e6c1fuL.toLong(),
            0x1f83d9abfb41bd6buL.toLong(),
            0x5be0cd19137e2179uL.toLong(),
        ]
    }

    private interface Sha2Stream {
        fun update(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): Sha2Stream
        fun finish(): ByteArray
        fun reset(): Sha2Stream
    }

    private class Sha256Stream(private val type: BitType) : Sha2Stream {
        private val state = IntArray(8)
        private val buffer = ByteArray(64)
        private var buffered = 0
        private var totalBytes = 0uL

        init {
            require(type == BitType.B224 || type == BitType.B256)
            reset()
        }

        override fun update(data: ByteArray, offset: Int, length: Int): Sha256Stream {
            require(offset >= 0 && length >= 0 && offset <= data.size - length)

            totalBytes += length.toULong()

            var cursor = offset
            val end = offset + length

            while (cursor < end) {
                val count = minOf(buffer.size - buffered, end - cursor)
                data.copyInto(buffer, buffered, cursor, cursor + count)
                buffered += count
                cursor += count

                if (buffered == buffer.size) {
                    compress(buffer)
                    buffer.fill(0)
                    buffered = 0
                }
            }

            return this
        }

        override fun finish(): ByteArray {
            val copy = Sha256Stream(type)
            state.copyInto(copy.state)
            buffer.copyInto(copy.buffer)
            copy.buffered = buffered
            copy.totalBytes = totalBytes
            copy.buffer[copy.buffered++] = 0x80.toByte()

            if (copy.buffered > 56) {
                copy.buffer.fill(0, copy.buffered, 64)
                copy.compress(copy.buffer)
                copy.buffer.fill(0)
                copy.buffered = 0
            }

            copy.buffer.fill(0, copy.buffered, 56)
            ByteArrayIO(copy.buffer).writeULong(56, copy.totalBytes shl 3, Endian.BIG)
            copy.compress(copy.buffer)

            return ByteArrayIO(type.bits / 8).write {
                for (i in 0 ..< type.bits / 32) {
                    writeInt(i * 4, copy.state[i], Endian.BIG)
                }
            }
        }

        private fun compress(block: ByteArray) {
            val words = IntArray(64)

            val blockIO = ByteArrayIO(block)
            for (i in 0 ..< 16) words[i] = blockIO.readInt(i * 4, Endian.BIG)

            for (i in 16 ..< 64) {
                val x = words[i - 15]
                val y = words[i - 2]

                val sigma0 = x.rotateRight(7) xor x.rotateRight(18) xor (x ushr 3)
                val sigma1 = y.rotateRight(17) xor y.rotateRight(19) xor (y ushr 10)
                words[i] = words[i - 16] + sigma0 + words[i - 7] + sigma1
            }

            var a = state[0]
            var b = state[1]
            var c = state[2]
            var d = state[3]
            var e = state[4]
            var f = state[5]
            var g = state[6]
            var h = state[7]

            for (i in 0 ..< 64) {
                val sigma1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
                val choice = (e and f) xor (e.inv() and g)
                val temp1 = h + sigma1 + choice + K256[i] + words[i]
                val sigma0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
                val majority = (a and b) xor (a and c) xor (b and c)
                val temp2 = sigma0 + majority

                h = g
                g = f
                f = e
                e = d + temp1
                d = c
                c = b
                b = a
                a = temp1 + temp2
            }

            state[0] += a
            state[1] += b
            state[2] += c
            state[3] += d
            state[4] += e
            state[5] += f
            state[6] += g
            state[7] += h
        }

        override fun reset(): Sha256Stream {
            when (type) {
                BitType.B224 -> Initial224
                BitType.B256 -> Initial256
                else -> error("Invalid SHA-2 type")
            }.copyInto(state)

            buffer.fill(0)
            buffered = 0
            totalBytes = 0uL

            return this
        }
    }

    private class Sha512Stream(private val type: BitType) : Sha2Stream {
        private val state = LongArray(8)
        private val buffer = ByteArray(128)
        private var buffered = 0
        private var totalBytes = 0uL

        init {
            require(type == BitType.B384 || type == BitType.B512)
            reset()
        }

        override fun update(data: ByteArray, offset: Int, length: Int): Sha512Stream {
            require(offset >= 0 && length >= 0 && offset <= data.size - length)

            totalBytes += length.toULong()

            var cursor = offset
            val end = offset + length

            while (cursor < end) {
                val count = minOf(buffer.size - buffered, end - cursor)

                data.copyInto(buffer, buffered, cursor, cursor + count)
                buffered += count
                cursor += count

                if (buffered == buffer.size) {
                    compress(buffer)
                    buffer.fill(0)
                    buffered = 0
                }
            }

            return this
        }

        override fun finish(): ByteArray {
            val copy = Sha512Stream(type)
            state.copyInto(copy.state)
            buffer.copyInto(copy.buffer)
            copy.buffered = buffered
            copy.totalBytes = totalBytes
            copy.buffer[copy.buffered++] = 0x80.toByte()

            if (copy.buffered > 112) {
                copy.buffer.fill(0, copy.buffered, 128)
                copy.compress(copy.buffer)
                copy.buffer.fill(0)
                copy.buffered = 0
            }

            copy.buffer.fill(0, copy.buffered, 112)

            val copyBufferIO = ByteArrayIO(copy.buffer)
            copyBufferIO.writeULong(112, copy.totalBytes shr 61, Endian.BIG)
            copyBufferIO.writeULong(120, copy.totalBytes shl 3, Endian.BIG)

            copy.compress(copy.buffer)

            return ByteArrayIO(type.bits / 8).write {
                for (i in 0 ..< type.bits / 61) {
                    writeLong(i * 8, copy.state[i], Endian.BIG)
                }
            }
        }

        private fun compress(block: ByteArray) {
            val words = LongArray(80)

            val blockIO = ByteArrayIO(block)
            for (i in 0 ..< 16) {
                words[i] = blockIO.readLong(i * 8, Endian.BIG)
            }

            for (i in 16 ..< 80) {
                val x = words[i - 15]
                val y = words[i - 2]
                val sigma0 = x.rotateRight(1) xor x.rotateRight(8) xor (x ushr 7)
                val sigma1 = y.rotateRight(19) xor y.rotateRight(61) xor (y ushr 6)
                words[i] = words[i - 16] + sigma0 + words[i - 7] + sigma1
            }

            var a = state[0]
            var b = state[1]
            var c = state[2]
            var d = state[3]
            var e = state[4]
            var f = state[5]
            var g = state[6]
            var h = state[7]

            for (i in 0 ..< 80) {
                val sigma1 = e.rotateRight(14) xor e.rotateRight(18) xor e.rotateRight(41)
                val choice = (e and f) xor (e.inv() and g)
                val temp1 = h + sigma1 + choice + K512[i] + words[i]
                val sigma0 = a.rotateRight(28) xor a.rotateRight(34) xor a.rotateRight(39)
                val majority = (a and b) xor (a and c) xor (b and c)
                val temp2 = sigma0 + majority

                h = g
                g = f
                f = e
                e = d + temp1
                d = c
                c = b
                b = a
                a = temp1 + temp2
            }

            state[0] += a
            state[1] += b
            state[2] += c
            state[3] += d
            state[4] += e
            state[5] += f
            state[6] += g
            state[7] += h
        }

        override fun reset(): Sha512Stream {
            when (type) {
                BitType.B384 -> Initial384
                BitType.B512 -> Initial512
                else -> error("Invalid SHA-2 type")
            }.copyInto(state)

            buffer.fill(0)
            buffered = 0
            totalBytes = 0uL

            return this
        }
    }

    private val stream: Sha2Stream = when (type) {
        BitType.B224, BitType.B256 -> Sha256Stream(type)
        BitType.B384, BitType.B512 -> Sha512Stream(type)
    }

    override fun encode(data: ByteArray): ByteArray {
        val result = stream.update(data).finish()
        stream.reset()
        return result
    }
}