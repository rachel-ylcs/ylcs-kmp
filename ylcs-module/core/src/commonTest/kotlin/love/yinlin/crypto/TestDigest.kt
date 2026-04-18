package love.yinlin.crypto

import kotlin.test.Test
import kotlin.test.assertEquals

class TestDigest {
    @Test
    fun testMD5() {
        assertEquals(MD5(is16Bit = false, isUppercase = false).encodeToString("Hello World"), "b10a8db164e0754105b7a99be72e3fe5")
        assertEquals(MD5(is16Bit = false, isUppercase = true).encodeToString("Hello World"), "B10A8DB164E0754105B7A99BE72E3FE5")
        assertEquals(MD5(is16Bit = true, isUppercase = false).encodeToString("Kotlin你好"), "7201c66022293f97")
    }
}