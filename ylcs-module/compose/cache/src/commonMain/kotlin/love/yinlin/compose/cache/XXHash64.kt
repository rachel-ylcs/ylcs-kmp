package love.yinlin.compose.cache

internal object XXHash64 {
    private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz"
    private val SEEDS = longArrayOf(
        0x395b586ca42e1612UL.toLong(),
        0x1234567890ABCDEFUL.toLong(),
        0x9876543210FEDCBAuL.toLong()
    )
    private val MASKS = longArrayOf(
        0xbf58476d1ce4e5b9UL.toLong(),
        0x94d049bb133111ebUL.toLong(),
        0xff51afd7ed558ccdUL.toLong()
    )
    private val LENGTHS = intArrayOf(11, 11, 10) // 11 + 11 + 10 = 32

    fun hash(input: String): String = buildString(32) {
        repeat(3) { index ->
            var h = SEEDS[index]
            for (char in input) {
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