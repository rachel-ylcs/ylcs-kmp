package love.yinlin.crypto

object XXHash64 : StringDigest() {
    private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz"

    private val SEEDS: LongArray = [
        0x395b586ca42e1612UL.toLong(),
        0x1234567890ABCDEFUL.toLong(),
        0x9876543210FEDCBAuL.toLong()
    ]

    private val MASKS: LongArray = [
        0xbf58476d1ce4e5b9UL.toLong(),
        0x94d049bb133111ebUL.toLong(),
        0xff51afd7ed558ccdUL.toLong()
    ]

    private val LENGTHS: IntArray = [11, 11, 10] // 11 + 11 + 10 = 32

    override fun encode(data: String): String = buildString(32) {
        repeat(3) { index ->
            var h = SEEDS[index]
            for (char in data) {
                h = h xor (char.code.toLong() * MASKS[index])
                h = h.rotateLeft(31)
                h *= 0xbf58476d1ce4e5b9uL.toLong()
            }
            h = h xor (h ushr 33)
            h *= -0xae502812aa7333L
            h = h xor (h ushr 33)
            h *= -0x3b3146010f6d7dL
            h = h xor (h ushr 33)
            var n = if (h < 0) -(h + 1) else h
            repeat(LENGTHS[index]) {
                append(ALPHABET[(n % 26).toInt()])
                n /= 26
                if (n == 0L) n = h xor 0x5555555555555555L
            }
        }
    }
}