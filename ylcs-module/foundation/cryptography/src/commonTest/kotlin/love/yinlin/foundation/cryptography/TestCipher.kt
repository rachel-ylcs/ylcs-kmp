package love.yinlin.foundation.cryptography

import love.yinlin.extension.enumForEach
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestCipher {
    @Test
    fun testAES() {
        val plaintext = "00112233445566778899aabbccddeeff".hexToByteArray()
        val vectors = [
            "000102030405060708090a0b0c0d0e0f" to "69c4e0d86a7b0430d8cdb78070b4c55a",
            "000102030405060708090a0b0c0d0e0f1011121314151617" to "dda97ca4864cdfe06eaf70a0ec0d7191",
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f" to "8ea2b7ca516745bfeafc49904b496089",
        ]
        for ([keyHex, expectedCiphertext] in vectors) {
            val aes = AES(keyHex.hexToByteArray(), padding = AES.Padding.NONE)
            assertEquals(expectedCiphertext, aes.encodeToHex(plaintext))
            assertContentEquals(plaintext, aes.decode(expectedCiphertext.hexToByteArray()))
        }
    }

    @Test
    fun testAESGeneratedKeys() {
        val plaintext = ByteArray(20) { (it * 17).toByte() }
        enumForEach { keySize: AES.KeySize ->
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

    @Test
    fun testXChaCha20() {
        val xChaCha20 = XChaCha20(key = "9d23bd4149cb979ccf3c5c94dd217e9808cb0e50cd0f67812235eaaf601d6232".hexToByteArray())
        val output = xChaCha20.encodeToHex("hello world!")
        val result = xChaCha20.decodeFromHexToString(output)
        assertEquals("hello world!", result)
    }

    @Test
    fun testXChaCha20Compatibility() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(24) { it.toByte() }
        val plaintext = ByteArray(192) { (it * 17 + 3).toByte() }
        val ciphertext = "e62e44f8b609816e8a9ad05df53f312c838f151536dce2d90476c2c0bc41bd6f4bcfab00aeba85a33db29481d1a77db5ab4085ea8dfa17cc7093ab857b1ee72ddd966a09174a2414f898cb30c47299aa18235d236a7ea421a4f6c2b8229db40b3d723372e6dfc0be3e69a347e0610200023e77fff0bc22f3fc978e13b0b7258631b45a05cf418d7f27430bff14057820caa4a95c64edc4934dbc0039ee61f2decdb0b09af0e879d1ce3170895c81ccfe83fb1a1d1153624ef23d03411819785b".hexToByteArray()
        val cipher = XChaCha20(key)
        for (size in [0, 1, 63, 64, 65, 127, 128, 129, 192]) {
            val input = plaintext.copyOf(size)
            assertContentEquals(input, cipher.decode(nonce + ciphertext.copyOf(size)), "libsodium size=$size")
            assertContentEquals(input, cipher.decode(cipher.encode(input)), "round trip size=$size")
        }
        assertFailsWith<IllegalArgumentException> { cipher.decode(ByteArray(23)) }
    }
}