package love.yinlin.foundation.cryptography

import kotlin.test.Test
import kotlin.test.assertContentEquals
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

    @Test
    fun sha2MatchesIndependentVectorsAfterReset() {
        // Python hashlib/OpenSSL 固定向量：2049 字节，data[i] = (17 * i + 3) mod 256。
        val vectors = [
            SHA2.BitType.B224 to "be972b59144c17bf28b17b64424aa363869ef6a414811b30e5447303",
            SHA2.BitType.B256 to "22f0a97231b1c2f1c8e4390f637461035e336295817ba70929f902e11b5e9809",
            SHA2.BitType.B384 to "536246b0c3bf80aafb35719faa3b0b7fbb33894151cc2461881dc733f42e7c007b533f98a986eee33e5c1dcef239f1f3",
            SHA2.BitType.B512 to "8d100d63b94232bc3aa2fccebbc1d81927081f43679c075b28cae512eaad766c698f3a98d70fce48858b69cf130464f2248bce152d6391218c722fe54acab66e",
        ]
        val data = ByteArray(2049) { (it * 17 + 3).toByte() }
        val original = data.copyOf()
        for ([type, expected] in vectors) {
            val digest = SHA2(type)
            assertEquals(expected, digest.encodeToHex(data))
            digest.encode(ByteArray(0))
            assertEquals(expected, digest.encodeToHex(data))
        }
        assertContentEquals(original, data)
    }

    @Test
    fun sha3MatchesIndependentVectorsAfterReset() {
        // Python hashlib/OpenSSL 固定向量：2049 字节，data[i] = (17 * i + 3) mod 256。
        val vectors = [
            SHA3.BitType.B224 to "a49a51472ba5ef44eab2982ade4daa158e8ceffc960f7d43c78e2a30",
            SHA3.BitType.B256 to "67ebda68568cbb920cdb949f1013207be94aaf2d4c6bf0f4468faa395138fbfc",
            SHA3.BitType.B384 to "2553dae965ccbe1141d1d5b2f21a4464e3d67552813524f13b7097cc67473b79016a0f6b15b775c923bde2a6044dccfa",
            SHA3.BitType.B512 to "340f8aec4b3b535d14c6155a555dc3ce8fd441f818fe39b606be0261b7be00e25fc693977c83d56230ba9450f10a516a2f1995c4be23e1d3768ca4d06ba611e8",
        ]
        val data = ByteArray(2049) { (it * 17 + 3).toByte() }
        val original = data.copyOf()
        for ([type, expected] in vectors) {
            val digest = SHA3(type)
            assertEquals(expected, digest.encodeToHex(data))
            digest.encode(ByteArray(0))
            assertEquals(expected, digest.encodeToHex(data))
        }
        assertContentEquals(original, data)
    }
}