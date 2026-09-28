package love.yinlin.foundation.cryptography

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
        var p = offset
        var i = 0

        while (i < 16) {
            w[i] = ((input[p].toInt() and 0xFF) shl 24) or
                    ((input[p + 1].toInt() and 0xFF) shl 16) or
                    ((input[p + 2].toInt() and 0xFF) shl 8) or
                    (input[p + 3].toInt() and 0xFF)
            p += 4
            i++
        }

        expandAndCompress()
    }

    private fun compressFinal(input: ByteArray, offset: Int, remaining: Int, bitLength: Long) {
        w.fill(0, 0, 16)

        var i = 0

        while (i < remaining) {
            val wordIndex = i ushr 2
            val shift = 24 - ((i and 3) shl 3)

            w[wordIndex] = w[wordIndex] or ((input[offset + i].toInt() and 0xFF) shl shift)
            i++
        }

        val paddingWord = remaining ushr 2
        val paddingShift = 24 - ((remaining and 3) shl 3)

        w[paddingWord] = w[paddingWord] or (0x80 shl paddingShift)

        if (remaining <= 55) {
            w[14] = (bitLength ushr 32).toInt()
            w[15] = bitLength.toInt()
            expandAndCompress()
        } else {
            expandAndCompress()
            w.fill(element = 0, fromIndex = 0, toIndex = 16)
            w[14] = (bitLength ushr 32).toInt()
            w[15] = bitLength.toInt()
            expandAndCompress()
        }
    }

    private fun writeInt(output: ByteArray, offset: Int, value: Int) {
        output[offset] = (value ushr 24).toByte()
        output[offset + 1] = (value ushr 16).toByte()
        output[offset + 2] = (value ushr 8).toByte()
        output[offset + 3] = value.toByte()
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

        val result = ByteArray(32)

        writeInt(result, 0, h0)
        writeInt(result, 4, h1)
        writeInt(result, 8, h2)
        writeInt(result, 12, h3)
        writeInt(result, 16, h4)
        writeInt(result, 20, h5)
        writeInt(result, 24, h6)
        writeInt(result, 28, h7)

        return result
    }
}