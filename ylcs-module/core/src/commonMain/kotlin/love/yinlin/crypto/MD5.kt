package love.yinlin.crypto

class MD5(private val is16Bit: Boolean = false, private val isUppercase: Boolean = false) : Digest {
    companion object {
        private val S = intArrayOf(
            7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
            5,  9, 14, 20, 5,  9, 14, 20, 5,  9, 14, 20, 5,  9, 14, 20,
            4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
            6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
        )

        private val T = intArrayOf(
            -680876936, -389564586, 606105819, -1044525330,
            -176418897, 1200080426, -1473231341, -45705983,
            1770035416, -1958414417, -42063, -1990404162,
            1804603682, -40341101, -1502002290, 1236535329,
            -165796510, -1069501632, 643717713, -373897302,
            -701558691, 38016083, -660478335, -405537848,
            568446438, -1019803690, -187363961, 1163531501,
            -1444681467, -51403784, 1735328473, -1926607734,
            -378558, -2022574463, 1839030562, -35309556,
            -1530992060, 1272893353, -155497632, -1094730640,
            681279174, -358537222, -722521979, 76029189,
            -640364487, -421815835, 530742520, -995338651,
            -198630844, 1126891415, -1416354905, -57434055,
            1700485571, -1894986606, -1051523, -2054922799,
            1873313359, -30611744, -1560198380, 1309151649,
            -145523070, -1120210379, 718787259, -343485551,
        )

        private const val HEX_CHARS_LOWER = "0123456789abcdef"
        private const val HEX_CHARS_UPPER = "0123456789ABCDEF"

        private fun calculate(input: ByteArray): ByteArray {
            val oldLen = input.size
            val newLen = ((oldLen + 8).ushr(6) shl 6) + 64
            val padding = ByteArray(newLen - oldLen)
            padding[0] = 0x80.toByte()

            val lengthBits = oldLen.toLong() * 8
            for (i in 0 .. 7) padding[padding.size - 8 + i] = (lengthBits ushr (i * 8)).toByte()

            var a = 0x67452301
            var b = 0xefcdab89.toInt()
            var c = 0x98badcfe.toInt()
            var d = 0x10325476

            val x = IntArray(16)
            for (offset in 0 until (oldLen + padding.size) step 64) {
                for (j in 0..15) {
                    var value = 0
                    for (k in 0..3) {
                        val bytePos = offset + j * 4 + k
                        val bVal = if (bytePos < oldLen) input[bytePos].toInt() else padding[bytePos - oldLen].toInt()
                        value = value or ((bVal and 0xFF) shl (k * 8))
                    }
                    x[j] = value
                }

                val aa = a
                var bb = b
                var cc = c
                var dd = d
                for (i in 0 .. 63) {
                    val div16 = i ushr 4
                    var f = 0
                    var g = 0
                    when (div16) {
                        0 -> {
                            f = (bb and cc) or (bb.inv() and dd)
                            g = i
                        }
                        1 -> {
                            f = (bb and dd) or (cc and dd.inv())
                            g = (5 * i + 1) % 16
                        }
                        2 -> {
                            f = bb xor cc xor dd
                            g = (3 * i + 5) % 16
                        }
                        3 -> {
                            f = cc xor (bb or dd.inv())
                            g = (7 * i) % 16
                        }
                    }
                    val temp = dd
                    dd = cc
                    cc = bb
                    val rot = a + f + T[i] + x[g]
                    val s = S[i]
                    bb += (rot shl s) or (rot ushr (32 - s))
                    a = temp
                }
                a += aa
                b += bb
                c += cc
                d += dd
            }

            val result = ByteArray(16)
            result[0] = (a and 0xFF).toByte()
            result[1] = (a ushr 8 and 0xFF).toByte()
            result[2] = (a ushr 16 and 0xFF).toByte()
            result[3] = (a ushr 24 and 0xFF).toByte()
            result[4] = (b and 0xFF).toByte()
            result[5] = (b ushr 8 and 0xFF).toByte()
            result[6] = (b ushr 16 and 0xFF).toByte()
            result[7] = (b ushr 24 and 0xFF).toByte()
            result[8] = (c and 0xFF).toByte()
            result[9] = (c ushr 8 and 0xFF).toByte()
            result[10] = (c ushr 16 and 0xFF).toByte()
            result[11] = (c ushr 24 and 0xFF).toByte()
            result[12] = (d and 0xFF).toByte()
            result[13] = (d ushr 8 and 0xFF).toByte()
            result[14] = (d ushr 16 and 0xFF).toByte()
            result[15] = (d ushr 24 and 0xFF).toByte()
            return result
        }

        private fun formatResult(digest: ByteArray, is16Bit: Boolean, isUppercase: Boolean): String {
            val hexTable = if (isUppercase) HEX_CHARS_UPPER else HEX_CHARS_LOWER
            val start = if (is16Bit) 4 else 0
            val end = if (is16Bit) 12 else 16
            return buildString(if (is16Bit) 16 else 32) {
                for (i in start ..< end) {
                    val b = digest[i].toInt() and 0xFF
                    append(hexTable[b ushr 4])
                    append(hexTable[b and 0x0F])
                }
            }
        }

        val Default = MD5()
    }

    override fun encode(data: ByteArray): ByteArray = calculate(data)

    fun encodeToString(data: ByteArray): String = formatResult(calculate(data), is16Bit, isUppercase)

    fun encodeToString(data: String): String = formatResult(calculate(data.encodeToByteArray()), is16Bit, isUppercase)
}