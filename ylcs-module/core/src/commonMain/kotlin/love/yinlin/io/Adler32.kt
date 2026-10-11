package love.yinlin.io

class Adler32 {
    private var a: Int = 1
    private var b: Int = 0

    val value: Int get() = (b shl 16) or a

    fun update(v: Byte): Adler32 {
        a += v.toInt() and 0xFF
        if (a >= MODULUS) a -= MODULUS
        b += a
        if (b >= MODULUS) b -= MODULUS
        return this
    }

    fun update(v1: Byte, v2: Byte): Adler32 {
        a += v1.toInt() and 0xFF
        if (a >= MODULUS) a -= MODULUS
        b += a
        if (b >= MODULUS) b -= MODULUS
        a += v2.toInt() and 0xFF
        if (a >= MODULUS) a -= MODULUS
        b += a
        if (b >= MODULUS) b -= MODULUS
        return this
    }

    fun update(v1: Byte, v2: Byte, v3: Byte): Adler32 {
        a += v1.toInt() and 0xFF
        if (a >= MODULUS) a -= MODULUS
        b += a
        if (b >= MODULUS) b -= MODULUS
        a += v2.toInt() and 0xFF
        if (a >= MODULUS) a -= MODULUS
        b += a
        if (b >= MODULUS) b -= MODULUS
        a += v3.toInt() and 0xFF
        if (a >= MODULUS) a -= MODULUS
        b += a
        if (b >= MODULUS) b -= MODULUS
        return this
    }

    fun update(vararg arr: Byte): Adler32 {
        for (v in arr) {
            a += v.toInt() and 0xFF
            if (a >= MODULUS) a -= MODULUS
            b += a
            if (b >= MODULUS) b -= MODULUS
        }
        return this
    }

    fun update(data: ByteArray, start: Int = 0, end: Int = data.size): Adler32 {
        require(start in 0 .. end && end <= data.size) {
            "Invalid range [$start, $end) for array of size ${data.size}"
        }

        var sumA = a
        var sumB = b
        var position = start

        while (position < end) {
            val blockEnd = position + minOf(end - position, BLOCK_SIZE)
            while (position < blockEnd) {
                sumA += data[position++].toInt() and 0xFF
                sumB += sumA
            }

            sumA %= MODULUS

            sumB = (sumB.toUInt() % MODULUS.toUInt()).toInt()
        }

        a = sumA
        b = sumB

        return this
    }

    fun reset(): Int {
        val result = (b shl 16) or a
        a = 1
        b = 0
        return result
    }

    companion object {
        fun calculate(data: ByteArray, start: Int = 0, end: Int = data.size): Int = Adler32().update(data, start, end).value

        private const val MODULUS: Int = 65521
        private const val BLOCK_SIZE: Int = 5552
    }
}