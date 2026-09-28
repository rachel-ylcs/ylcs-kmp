package love.yinlin.foundation.cryptography

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TestCipher {
    @Test
    fun testAES() {
        val plaintext = "00112233445566778899aabbccddeeff".hexToByteArray()
        val vectors = listOf(
            "000102030405060708090a0b0c0d0e0f" to "69c4e0d86a7b0430d8cdb78070b4c55a",
            "000102030405060708090a0b0c0d0e0f1011121314151617" to "dda97ca4864cdfe06eaf70a0ec0d7191",
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f" to "8ea2b7ca516745bfeafc49904b496089",
        )
        for ([keyHex, expectedCiphertext] in vectors) {
            val aes = AES(keyHex.hexToByteArray(), padding = AES.Padding.NONE)
            assertEquals(expectedCiphertext, aes.encodeToHex(plaintext))
            assertContentEquals(plaintext, aes.decode(expectedCiphertext.hexToByteArray()))
        }
    }

    @Test
    fun testAESGeneratedKeys() {
        val plaintext = ByteArray(20) { (it * 17).toByte() }
        for (keySize in AES.KeySize.entries) {
            val key = AES.generateKey(keySize)
            assertEquals(keySize.bytes, key.size)
            val aes = AES(key)
            assertContentEquals(plaintext, aes.decode(aes.encode(plaintext)))
        }
        assertEquals(AES.KeySize.B256.bytes, AES.generateKey().size)
    }

    @Test
    fun testRC4() {
        val rc4 = RC4(key = "daniel123456^#*@reyv")
        val input = "{abcdssdsadasdadasdsadasda123456}"
        val output = rc4.encodeToHex(input)
        val result = rc4.decodeFromHexToString(output)
        assertEquals("cac70df5bf3fb1b2d10f3db4ca75778ce8496499183e5ca22c5beecabbe2fed18d", output)
        assertEquals(input, result)
    }
}