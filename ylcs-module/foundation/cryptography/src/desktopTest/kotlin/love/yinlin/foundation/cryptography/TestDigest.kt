package love.yinlin.foundation.cryptography

import kotlin.test.Test
import kotlin.test.assertEquals

class TestDigest {
    @Test
    fun testMD5() {
        assertEquals("b10a8db164e0754105b7a99be72e3fe5", MD5.Default.encodeToHex("Hello World"))
        assertEquals("B10A8DB164E0754105B7A99BE72E3FE5", MD5.Default.encodeToHex("Hello World", HexFormat.UpperCase))
        assertEquals("7201c66022293f97", MD5(mini = true).encodeToHex("Kotlin你好"))
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
    fun testRC4() {
        val rc4 = RC4(key = "daniel123456^#*@reyv")
        val input = "{abcdssdsadasdadasdsadasda123456}"
        val output = rc4.encodeToHex(input)
        val result = rc4.decodeFromHexToString(output)
        assertEquals("cac70df5bf3fb1b2d10f3db4ca75778ce8496499183e5ca22c5beecabbe2fed18d", output)
        assertEquals(input, result)
    }
}