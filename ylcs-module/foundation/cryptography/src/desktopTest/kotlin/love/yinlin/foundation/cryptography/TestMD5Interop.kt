package love.yinlin.foundation.cryptography

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class TestMD5Interop {
    @Test
    fun matchesJdkAcrossPaddingAndBlockBoundaries() {
        for (size in [0, 1, 55, 56, 63, 64, 65, 119, 120, 127, 128, 129, 4096, 1024 * 1024]) {
            val input = ByteArray(size) { (it * 31 + 7).toByte() }
            val original = input.copyOf()
            val expected = MessageDigest.getInstance("MD5").digest(input)
            assertContentEquals(expected, MD5.Default.encode(input), "size=$size")
            assertContentEquals(expected.copyOfRange(4, 12), MD5(half = true).encode(input), "half size=$size")
            assertContentEquals(original, input)
        }
    }
}