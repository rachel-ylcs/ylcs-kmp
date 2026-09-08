package love.yinlin.crypto

import kotlin.test.Test
import kotlin.test.assertEquals

class TestDigest {
    @Test
    fun testMD5() {
        assertEquals("b10a8db164e0754105b7a99be72e3fe5", MD5(is16Bit = false, isUppercase = false).encodeToString("Hello World"))
        assertEquals("B10A8DB164E0754105B7A99BE72E3FE5", MD5(is16Bit = false, isUppercase = true).encodeToString("Hello World"))
        assertEquals("7201c66022293f97", MD5(is16Bit = true, isUppercase = false).encodeToString("Kotlin你好"))
    }

    @Test
    fun testXXHash64() {
        listOf(
            "hello world",
            "hello world!",
            "love.yinlin",
            "XXHash64XXHash64XXHash64XXHash64XXHash64XXHash64XXHash64",
        ).forEach {
            println("$it -> ${XXHash64.encodeToString(it)}")
        }
    }
}