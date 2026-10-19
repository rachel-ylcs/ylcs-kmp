package love.yinlin.foundation.cryptography

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class TestSHAInterop {
    private val sizes: List<Int> = [0, 1, 55, 56, 63, 64, 65, 71, 72, 103, 104, 111, 112, 127, 128, 129, 135, 136, 143, 144, 145, 4097, 1048576]

    @Test
    fun sha2MatchesJdkAcrossPaddingAndReuseBoundaries() {
        for (type in SHA2.BitType.entries) {
            val digest = SHA2(type)
            val reference = MessageDigest.getInstance("SHA-${type.bits}")
            for (size in sizes) {
                val data = ByteArray(size) { (it * 17 + 3).toByte() }
                val snapshot = data.copyOf()
                assertContentEquals(reference.digest(data), digest.encode(data), "SHA-${type.bits} size=$size")
                assertContentEquals(snapshot, data)
            }
        }
    }

    @Test
    fun sha3MatchesJdkAcrossEveryRateBoundary() {
        for (type in SHA3.BitType.entries) {
            val digest = SHA3(type)
            val reference = MessageDigest.getInstance("SHA3-${type.bits}")
            for (size in sizes) {
                val data = ByteArray(size) { (it * 17 + 3).toByte() }
                val snapshot = data.copyOf()
                assertContentEquals(reference.digest(data), digest.encode(data), "SHA3-${type.bits} size=$size")
                assertContentEquals(snapshot, data)
            }
        }
    }
}