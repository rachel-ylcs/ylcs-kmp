package love.yinlin.foundation.cryptography

import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

class AES(
    key: ByteArray,
    val mode: Mode = Mode.ECB,
    val padding: Padding = Padding.PKCS7,
) : Cipher() {
    enum class KeySize(val bytes: Int) {
        B128(16),
        B192(24),
        B256(32);
    }

    enum class Mode {
        ECB;
    }

    enum class Padding {
        PKCS7,
        NONE
    }

    companion object {
        fun generateKey(keySize: KeySize = KeySize.B256): ByteArray = secureRandomBytes(keySize.bytes)

        private const val BLOCK_SIZE = 16

        private val SBOX: IntArray = [
            0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
            0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
            0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
            0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
            0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
            0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
            0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
            0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
            0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
            0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
            0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
            0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
            0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
            0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
            0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
            0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16,
        ]

        private val INVERSE_SBOX = IntArray(256).also { inverse ->
            for (i in SBOX.indices) inverse[SBOX[i]] = i
        }

        private val TE0 = IntArray(256) { i ->
            val value = SBOX[i]
            (xtime(value) shl 24) or (value shl 16) or (value shl 8) or (xtime(value) xor value)
        }
        private val TE1 = IntArray(256) { TE0[it].rotateRight(8) }
        private val TE2 = IntArray(256) { TE1[it].rotateRight(8) }
        private val TE3 = IntArray(256) { TE2[it].rotateRight(8) }

        private val TD0 = IntArray(256) { i ->
            val value = INVERSE_SBOX[i]
            (multiply(value, 14) shl 24) or (multiply(value, 9) shl 16) or (multiply(value, 13) shl 8) or multiply(value, 11)
        }
        private val TD1 = IntArray(256) { TD0[it].rotateRight(8) }
        private val TD2 = IntArray(256) { TD1[it].rotateRight(8) }
        private val TD3 = IntArray(256) { TD2[it].rotateRight(8) }

        private fun xtime(value: Int): Int = ((value shl 1) xor (if ((value and 0x80) != 0) 0x1b else 0)) and 0xff

        private fun multiply(value: Int, factor: Int): Int {
            var result = 0
            var term = value
            var bits = factor
            while (bits != 0) {
                if ((bits and 1) != 0) result = result xor term
                term = xtime(term)
                bits = bits ushr 1
            }
            return result
        }

        private fun substituteWord(word: Int): Int =
            (SBOX[word ushr 24] shl 24) or
                    (SBOX[(word ushr 16) and 0xff] shl 16) or
                    (SBOX[(word ushr 8) and 0xff] shl 8) or
                    SBOX[word and 0xff]

        private fun finalWord(a: Int, b: Int, c: Int, d: Int, box: IntArray): Int =
            (box[a ushr 24] shl 24) or
                    (box[(b ushr 16) and 0xff] shl 16) or
                    (box[(c ushr 8) and 0xff] shl 8) or
                    box[d and 0xff]

        private fun inverseMixWord(word: Int): Int {
            val a = word ushr 24
            val b = (word ushr 16) and 0xff
            val c = (word ushr 8) and 0xff
            val d = word and 0xff
            return ((multiply(a, 14) xor multiply(b, 11) xor multiply(c, 13) xor multiply(d, 9)) shl 24) or
                    ((multiply(a, 9) xor multiply(b, 14) xor multiply(c, 11) xor multiply(d, 13)) shl 16) or
                    ((multiply(a, 13) xor multiply(b, 9) xor multiply(c, 14) xor multiply(d, 11)) shl 8) or
                    (multiply(a, 11) xor multiply(b, 13) xor multiply(c, 9) xor multiply(d, 14))
        }
    }

    private val rounds = when (key.size) {
        16 -> 10
        24 -> 12
        32 -> 14
        else -> throw IllegalArgumentException("AES key must contain 16, 24 or 32 bytes")
    }

    private val encryptionKeys = IntArray((rounds + 1) * 4).also { words ->
        val io = ByteArrayIO(key)
        val keyWords = key.size / 4

        for (i in 0 ..< keyWords) words[i] = io.readInt(i * 4, Endian.BIG)

        var rcon = 1
        for (i in keyWords ..< words.size) {
            var previous = words[i - 1]
            if (i % keyWords == 0) {
                previous = substituteWord((previous shl 8) or (previous ushr 24)) xor (rcon shl 24)
                rcon = xtime(rcon)
            } else if (keyWords == 8 && i % keyWords == 4) {
                previous = substituteWord(previous)
            }
            words[i] = words[i - keyWords] xor previous
        }
    }

    private val decryptionKeys = IntArray(encryptionKeys.size).also { words ->
        for (round in 0..rounds) {
            for (column in 0..3) {
                val word = encryptionKeys[(rounds - round) * 4 + column]
                words[round * 4 + column] = if (round == 0 || round == rounds) word else inverseMixWord(word)
            }
        }
    }

    private fun encodeEcb(data: ByteArray): ByteArray {
        val fullSize = data.size - data.size % BLOCK_SIZE
        val outputSize = when (padding) {
            Padding.NONE -> {
                require(fullSize == data.size) { "AES without padding requires complete blocks" }
                data.size
            }
            Padding.PKCS7 -> {
                require(data.size <= Int.MAX_VALUE - BLOCK_SIZE) { "AES input is too large" }
                fullSize + BLOCK_SIZE
            }
        }
        val output = ByteArray(outputSize)
        var offset = 0
        while (offset < fullSize) {
            encryptBlock(data, offset, output, offset)
            offset += BLOCK_SIZE
        }
        if (padding == Padding.PKCS7) {
            val padSize = BLOCK_SIZE - (data.size - fullSize)
            val lastBlock = ByteArray(BLOCK_SIZE) { padSize.toByte() }
            data.copyInto(lastBlock, 0, fullSize, data.size)
            encryptBlock(lastBlock, 0, output, fullSize)
        }
        return output
    }

    private fun decodeEcb(data: ByteArray): ByteArray {
        require(data.size % BLOCK_SIZE == 0) { "AES ciphertext must contain complete blocks" }
        if (padding == Padding.NONE) {
            val output = ByteArray(data.size)
            var offset = 0
            while (offset < data.size) {
                decryptBlock(data, offset, output, offset)
                offset += BLOCK_SIZE
            }
            return output
        }

        require(data.isNotEmpty()) { "AES ciphertext with PKCS#7 padding cannot be empty" }
        val lastBlock = ByteArray(BLOCK_SIZE)
        val lastOffset = data.size - BLOCK_SIZE
        decryptBlock(data, lastOffset, lastBlock, 0)

        val padSize = lastBlock[BLOCK_SIZE - 1].toInt() and 0xff
        var invalidPadding = if (padSize in 1..BLOCK_SIZE) 0 else 1
        for (i in 0 until BLOCK_SIZE) {
            val mask = if (i < padSize) 0xff else 0
            invalidPadding = invalidPadding or (((lastBlock[BLOCK_SIZE - 1 - i].toInt() and 0xff) xor padSize) and mask)
        }
        require(invalidPadding == 0) { "Invalid AES PKCS#7 padding" }

        val output = ByteArray(data.size - padSize)
        var offset = 0
        while (offset < lastOffset) {
            decryptBlock(data, offset, output, offset)
            offset += BLOCK_SIZE
        }
        lastBlock.copyInto(output, lastOffset, 0, BLOCK_SIZE - padSize)
        return output
    }

    private fun encryptBlock(input: ByteArray, inputOffset: Int, output: ByteArray, outputOffset: Int) {
        val io = ByteArrayIO(input)
        val endian = Endian.BIG
        var s0 = io.readInt(inputOffset, endian) xor encryptionKeys[0]
        var s1 = io.readInt(inputOffset + 4, endian) xor encryptionKeys[1]
        var s2 = io.readInt(inputOffset + 8, endian) xor encryptionKeys[2]
        var s3 = io.readInt(inputOffset + 12, endian) xor encryptionKeys[3]

        for (round in 1 until rounds) {
            val keyOffset = round * 4
            val t0 = TE0[s0 ushr 24] xor TE1[(s1 ushr 16) and 0xff] xor TE2[(s2 ushr 8) and 0xff] xor TE3[s3 and 0xff] xor encryptionKeys[keyOffset]
            val t1 = TE0[s1 ushr 24] xor TE1[(s2 ushr 16) and 0xff] xor TE2[(s3 ushr 8) and 0xff] xor TE3[s0 and 0xff] xor encryptionKeys[keyOffset + 1]
            val t2 = TE0[s2 ushr 24] xor TE1[(s3 ushr 16) and 0xff] xor TE2[(s0 ushr 8) and 0xff] xor TE3[s1 and 0xff] xor encryptionKeys[keyOffset + 2]
            val t3 = TE0[s3 ushr 24] xor TE1[(s0 ushr 16) and 0xff] xor TE2[(s1 ushr 8) and 0xff] xor TE3[s2 and 0xff] xor encryptionKeys[keyOffset + 3]
            s0 = t0
            s1 = t1
            s2 = t2
            s3 = t3
        }

        val keyOffset = rounds * 4
        ByteArrayIO(output).write {
            writeInt(outputOffset, finalWord(s0, s1, s2, s3, SBOX) xor encryptionKeys[keyOffset], endian)
            writeInt(outputOffset + 4, finalWord(s1, s2, s3, s0, SBOX) xor encryptionKeys[keyOffset + 1], endian)
            writeInt(outputOffset + 8, finalWord(s2, s3, s0, s1, SBOX) xor encryptionKeys[keyOffset + 2], endian)
            writeInt(outputOffset + 12, finalWord(s3, s0, s1, s2, SBOX) xor encryptionKeys[keyOffset + 3], endian)
        }
    }

    private fun decryptBlock(input: ByteArray, inputOffset: Int, output: ByteArray, outputOffset: Int) {
        val io = ByteArrayIO(input)
        val endian = Endian.BIG
        var s0 = io.readInt(inputOffset, endian) xor decryptionKeys[0]
        var s1 = io.readInt(inputOffset + 4, endian) xor decryptionKeys[1]
        var s2 = io.readInt(inputOffset + 8, endian) xor decryptionKeys[2]
        var s3 = io.readInt(inputOffset + 12, endian) xor decryptionKeys[3]

        for (round in 1 until rounds) {
            val keyOffset = round * 4
            val t0 = TD0[s0 ushr 24] xor TD1[(s3 ushr 16) and 0xff] xor TD2[(s2 ushr 8) and 0xff] xor TD3[s1 and 0xff] xor decryptionKeys[keyOffset]
            val t1 = TD0[s1 ushr 24] xor TD1[(s0 ushr 16) and 0xff] xor TD2[(s3 ushr 8) and 0xff] xor TD3[s2 and 0xff] xor decryptionKeys[keyOffset + 1]
            val t2 = TD0[s2 ushr 24] xor TD1[(s1 ushr 16) and 0xff] xor TD2[(s0 ushr 8) and 0xff] xor TD3[s3 and 0xff] xor decryptionKeys[keyOffset + 2]
            val t3 = TD0[s3 ushr 24] xor TD1[(s2 ushr 16) and 0xff] xor TD2[(s1 ushr 8) and 0xff] xor TD3[s0 and 0xff] xor decryptionKeys[keyOffset + 3]
            s0 = t0
            s1 = t1
            s2 = t2
            s3 = t3
        }

        val keyOffset = rounds * 4
        ByteArrayIO(output).write {
            writeInt(outputOffset, finalWord(s0, s3, s2, s1, INVERSE_SBOX) xor decryptionKeys[keyOffset], endian)
            writeInt(outputOffset + 4, finalWord(s1, s0, s3, s2, INVERSE_SBOX) xor decryptionKeys[keyOffset + 1], endian)
            writeInt(outputOffset + 8, finalWord(s2, s1, s0, s3, INVERSE_SBOX) xor decryptionKeys[keyOffset + 2], endian)
            writeInt(outputOffset + 12, finalWord(s3, s2, s1, s0, INVERSE_SBOX) xor decryptionKeys[keyOffset + 3], endian)
        }
    }

    override fun encode(data: ByteArray): ByteArray = when (mode) {
        Mode.ECB -> encodeEcb(data)
    }

    override fun decode(data: ByteArray): ByteArray = when (mode) {
        Mode.ECB -> decodeEcb(data)
    }
}