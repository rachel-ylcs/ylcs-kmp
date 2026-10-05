package love.yinlin.foundation.cryptography

import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

class SM3 : ByteDigest() {
    private companion object {
        val ROT: IntArray = IntArray(64) { i ->
            (if (i < 16) 0x79CC4519 else 0x7A879D8A).rotateLeft(i)
        }
    }

    private val w = IntArray(68)

    private var h0 = 0
    private var h1 = 0
    private var h2 = 0
    private var h3 = 0
    private var h4 = 0
    private var h5 = 0
    private var h6 = 0
    private var h7 = 0

    private fun reset() {
        h0 = 0x7380166F
        h1 = 0x4914B2B9
        h2 = 0x172442D7
        h3 = 0xDA8A0600.toInt()
        h4 = 0xA96F30BC.toInt()
        h5 = 0x163138AA
        h6 = 0xE38DEE4D.toInt()
        h7 = 0xB0FB0E4E.toInt()
    }

    private fun expandAndCompress() {
        val w = this.w
        var j = 16
        while (j < 68) {
            val x = w[j - 16] xor w[j - 9] xor w[j - 3].rotateLeft(15)
            w[j] = x xor x.rotateLeft(15) xor x.rotateLeft(23) xor w[j - 13].rotateLeft(7) xor w[j - 6]
            j++
        }

        var a = h0
        var b = h1
        var c = h2
        var d = h3
        var e = h4
        var f = h5
        var g = h6
        var h = h7

        j = 0
        while (j < 16) {
            val a12 = a.rotateLeft(12)
            val ss1 = (a12 + e + ROT[j]).rotateLeft(7)
            val ss2 = ss1 xor a12
            val tt1 = (a xor b xor c) + d + ss2 + (w[j] xor w[j + 4])
            val tt2 = (e xor f xor g) + h + ss1 + w[j]
            d = c
            c = b.rotateLeft(9)
            b = a
            a = tt1
            h = g
            g = f.rotateLeft(19)
            f = e
            e = tt2 xor tt2.rotateLeft(9) xor tt2.rotateLeft(17)
            j++
        }

        while (j < 64) {
            val a12 = a.rotateLeft(12)
            val ss1 = (a12 + e + ROT[j]).rotateLeft(7)
            val ss2 = ss1 xor a12
            val ff = (a and b) or (c and (a xor b))
            val gg = g xor (e and (f xor g))
            val tt1 = ff + d + ss2 + (w[j] xor w[j + 4])
            val tt2 = gg + h + ss1 + w[j]

            d = c
            c = b.rotateLeft(9)
            b = a
            a = tt1

            h = g
            g = f.rotateLeft(19)
            f = e
            e = tt2 xor tt2.rotateLeft(9) xor tt2.rotateLeft(17)

            j++
        }

        h0 = h0 xor a
        h1 = h1 xor b
        h2 = h2 xor c
        h3 = h3 xor d

        h4 = h4 xor e
        h5 = h5 xor f
        h6 = h6 xor g
        h7 = h7 xor h
    }

    private fun compressBlock(input: ByteArray, offset: Int) {
        val inputIO = ByteArrayIO(input)
        for (i in 0 ..< 16) w[i] = inputIO.readInt(offset + i * 4, Endian.BIG)
        expandAndCompress()
    }

    private fun compressFinal(input: ByteArray, offset: Int, remaining: Int, bitLength: Long) {
        val size = if (remaining <= 55) 64 else 128
        val final = ByteArray(size)
        input.copyInto(destination = final, destinationOffset = 0, startIndex = offset, endIndex = offset + remaining)
        final[remaining] = 0x80.toByte()

        val finalIO = ByteArrayIO(final)
        finalIO.writeLong(size - 8, bitLength, Endian.BIG)
        compressBlock(final, 0)
        if (size == 128) compressBlock(final, 64)
    }

    override fun encode(data: ByteArray): ByteArray {
        reset()

        val length = data.size
        val fullBlockEnd = length and -64

        var offset = 0

        while (offset < fullBlockEnd) {
            compressBlock(data, offset)
            offset += 64
        }

        compressFinal(input = data, offset = offset, remaining = length - offset, bitLength = length.toLong() shl 3)

        return ByteArrayIO(32).write {
            writeInt(0, h0, Endian.BIG)
            writeInt(4, h1, Endian.BIG)
            writeInt(8, h2, Endian.BIG)
            writeInt(12, h3, Endian.BIG)
            writeInt(16, h4, Endian.BIG)
            writeInt(20, h5, Endian.BIG)
            writeInt(24, h6, Endian.BIG)
            writeInt(28, h7, Endian.BIG)
        }
    }
}