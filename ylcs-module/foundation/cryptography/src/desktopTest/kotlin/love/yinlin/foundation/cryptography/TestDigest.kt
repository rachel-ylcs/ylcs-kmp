package love.yinlin.foundation.cryptography

import kotlin.test.Test
import kotlin.test.assertEquals

class TestDigest {
    @Test
    fun testMD5() {
        assertEquals("b10a8db164e0754105b7a99be72e3fe5", MD5.Default.encodeToHex("Hello World"))
        assertEquals("B10A8DB164E0754105B7A99BE72E3FE5", MD5.Default.encodeToHex("Hello World", HexFormat.UpperCase))
        assertEquals("7201c66022293f97", MD5(half = true).encodeToHex("Kotlin你好"))
    }

    @Test
    fun testXXHash64() {
        [
            "hello world",
            "hello world!",
            "love.yinlin",
            "XXHash64XXHash64XXHash64XXHash64XXHash64XXHash64XXHash64",
        ].forEach {
            println("$it -> ${XXHash64.encode(it)}")
        }
    }

    @Test
    fun testSM3() {
        assertEquals("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0", SM3().encodeToHex("abc"))
    }

    @Test
    fun testSHA2() {
        val sha2 = SHA2()
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha2.encodeToHex("abc"))
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha2.encodeToHex(""))
        assertEquals("cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7", SHA2(type = SHA2.BitType.B384).encodeToHex("abc"))
    }

    @Test
    fun testSHA3() {
        val sha3 = SHA3()
        assertEquals("3a985da74fe225b2045c172d6bd390bd855f086e3e9d525b46bfe24511431532", sha3.encodeToHex("abc"))
        assertEquals("a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a", sha3.encodeToHex(""))
        assertEquals("ec01498288516fc926459f58e2c6ad8df9b473cb0fc08c2596da7cf0e49be4b298d88cea927ac7f539f1edf228376d25", SHA3(type = SHA3.BitType.B384).encodeToHex("abc"))
    }
}