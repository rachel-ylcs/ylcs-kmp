package love.yinlin.foundation.parser.arguments

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestArgumentsErrors {
    @Test
    fun invalidArgumentSyntax() {
        for (source in ["=value", "=", "a=b=c", "a==", "a=\"value\"suffix", "a=\"value\"=x", "a=x\"y\"", "a\"b=1", "a=\"", "a=\"unterminated", "a=\"escaped\\\"", "\"\"", "\"unterminated", "\"key\"=1"]) {
            assertFailsWith<ArgumentsParseException>(source) { Arguments(source) }
            assertFailsWith<ArgumentsParseException>(source) { Arguments([source]) }
        }
    }

    @Test
    fun whitespaceBeforeEqualsIsInvalid() {
        for (source in ["a =b", "a = b", "a\t=1", "a\n=1"])
            assertFailsWith<ArgumentsParseException>(source) { Arguments(source) }
    }

    @Test
    fun quotesCannotEscapeOutsideStringsOrQuoteKeys() {
        for (source in ["a=\\\"value", "a=prefix\\\"", "a=1 \"flag\"", "a=1 \"key\"=2", "a=1 flag\"x"])
            assertFailsWith<ArgumentsParseException>(source) { Arguments(source) }
    }

    @Test
    fun arrayElementsCannotContainSeveralArguments() {
        for (source in ["", " ", "\ta=1", "a=1 ", "a=1 b=2", "a= b", "a =1", "a=\"x\" b", "flag other", "a='x y'"]) {
            val error = assertFailsWith<ArgumentsParseException>(source) { Arguments(["tool.exe", source]) }
            assertEquals(1, error.argumentIndex)
        }
        assertFailsWith<ArgumentsParseException> { Arguments(["\"tool.exe\" a=1"]) }
    }

    @Test
    fun parseErrorsIdentifyOffsets() {
        val equals = assertFailsWith<ArgumentsParseException> { Arguments("a=b=c") }
        assertEquals(3, equals.offset)
        assertNull(equals.argumentIndex)
        val quote = assertFailsWith<ArgumentsParseException> { Arguments("ok=1 x=\"unfinished") }
        assertEquals(7, quote.offset)
        val array = assertFailsWith<ArgumentsParseException> { Arguments(["tool.exe", "ok=1", "x=y=z"]) }
        assertEquals(3, array.offset)
        assertEquals(2, array.argumentIndex)
        assertTrue(array.message!!.contains("argument 2"))
        val suffix = assertFailsWith<ArgumentsParseException> { Arguments("x=\"v\"z") }
        assertEquals(5, suffix.offset)
    }

    @Test
    fun trailingBackslashEscapesClosingQuote() {
        assertFailsWith<ArgumentsParseException> { Arguments("path=\"C:\\folder\\\"") }
        assertEquals("C:\\folder\\", Arguments("path=C:\\folder\\")["path"].String)
    }
}
