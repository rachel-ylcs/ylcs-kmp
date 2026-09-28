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
}