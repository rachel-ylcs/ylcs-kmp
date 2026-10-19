package love.yinlin.uri

import kotlin.test.Test
import kotlin.test.assertEquals

class TestUri {
    @Test
    fun encodesQueryValuesAsUtf8() {
        assertEquals("name%3D%E9%93%B6%E4%B8%B4%26page%3D1", Uri.encodeUri("name=银临&page=1"))
        assertEquals("a%20b%2Bc%2F%25%3F%23", Uri.encodeUri("a b+c/%?#"))
        assertEquals("AZaz09_-!.~'()*", Uri.encodeUri("AZaz09_-!.~'()*"))
        assertEquals("", Uri.encodeUri(""))
    }

    @Test
    fun roundTripsUnicodeAndReservedCharacters() {
        for (text in ["银临茶舍", "hello world & 你好", "🎵+100%", "a/b?key=value#fragment"]) {
            assertEquals(text, Uri.decodeUri(Uri.encodeUri(text)))
        }
    }

    @Test
    fun invalidEscapesDoNotInsertPartialBytes() {
        assertEquals("\uFFFD1", Uri.decodeUri("%G1"))
        assertEquals("\uFFFD", Uri.decodeUri("%1G"))
        assertEquals("a\uFFFD0b", Uri.decodeUri("a%G0b"))
        assertEquals("+", Uri.decodeUri("+"))
        assertEquals("\uFFFD", Uri.decodeUri("%"))
    }
}