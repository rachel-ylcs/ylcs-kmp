package love.yinlin.foundation.cryptography

import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

class MD5(private val half: Boolean = false) : ByteDigest() {
    companion object {
        private val S: IntArray = [
            7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
            5,  9, 14, 20, 5,  9, 14, 20, 5,  9, 14, 20, 5,  9, 14, 20,
            4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
            6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
        ]

        private val T: IntArray = [
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
        ]

        private fun calculate(input: ByteArray): ByteArray {
            val oldLen = input.size
            val newLen = ((oldLen + 8).ushr(6) shl 6) + 64
            val padded = ByteArray(newLen)

            input.copyInto(padded)
            padded[oldLen] = 0x80.toByte()
            ByteArrayIO(padded).writeLong(newLen - 8, oldLen.toLong() shl 3, Endian.LITTLE)

            var a = 0x67452301
            var b = 0xefcdab89.toInt()
            var c = 0x98badcfe.toInt()
            var d = 0x10325476

            val inputIO = ByteArrayIO(padded)
            val x = IntArray(16)
            for (offset in padded.indices step 64) {
                for (j in 0 .. 15) x[j] = inputIO.readInt(offset + j * 4, Endian.LITTLE)

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

            return ByteArrayIO(16).write {
                writeInt(0, a, Endian.LITTLE)
                writeInt(4, b, Endian.LITTLE)
                writeInt(8, c, Endian.LITTLE)
                writeInt(12, d, Endian.LITTLE)
            }
        }

        val Default = MD5()
    }

    override fun encode(data: ByteArray): ByteArray {
        val rawData = calculate(data)
        return if (half) rawData.copyOfRange(4, 12) else rawData
    }
}