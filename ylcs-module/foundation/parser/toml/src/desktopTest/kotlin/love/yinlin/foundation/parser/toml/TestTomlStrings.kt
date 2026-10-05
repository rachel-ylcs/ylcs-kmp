package love.yinlin.foundation.parser.toml

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestTomlStrings {
    private fun read(value: String, toml: Toml = Toml.Default): String = toml.parse("value = $value").Object["value"].String

    @Test
    fun basicAndLiteralStrings() {
        assertEquals("I'm a string. \"quoted\"", read("\"I'm a string. \\\"quoted\\\"\""))
        assertEquals("C:\\Users\\nodejs\\templates", read("'C:\\Users\\nodejs\\templates'"))
        assertEquals("# not a comment", read("'# not a comment'"))
        assertEquals("", read("\"\""))
        assertEquals("", read("''"))
    }

    @Test
    fun basicEscapes() {
        assertEquals("\b\t\n\u000c\r\"\\", read("\"\\b\\t\\n\\f\\r\\\"\\\\\""))
        assertEquals("你好🌍", read("\"\\u4f60\\u597d\\U0001f30d\""))
        assertEquals("\u0000\u007f", read("\"\\u0000\\u007f\""))
    }

    @Test
    fun version11Escapes() {
        assertEquals("\u001bJosé", read("\"\\eJos\\xE9\""))
        val strict = Toml(TomlConfiguration(version = TomlVersion.V1_0))
        assertFailsWith<TomlParseException> { read("\"\\e\"", strict) }
        assertFailsWith<TomlParseException> { read("\"\\x41\"", strict) }
    }

    @Test
    fun multilineBasicStrings() {
        assertEquals("Roses are red\nViolets are blue", read("\"\"\"\nRoses are red\nViolets are blue\"\"\""))
        assertEquals(" leading\n", read("\"\"\" leading\n\"\"\""))
        assertEquals("", read("\"\"\"\"\"\""))
        assertEquals("\n", read("\"\"\"\n\n\"\"\""))
    }

    @Test
    fun multilineLiteralStrings() {
        assertEquals("I don't need \\d{2}", read("'''I don't need \\d{2}'''"))
        assertEquals("one\ntwo\n", read("'''\none\ntwo\n'''"))
        assertEquals("", read("''''''"))
    }

    @Test
    fun multilineQuoteRuns() {
        assertEquals("\"quoted\"", read("\"\"\"\"quoted\"\"\"\""))
        assertEquals("\"\"quoted\"\"", read("\"\"\"\"\"quoted\"\"\"\"\""))
        assertEquals("'quoted'", read("''''quoted''''"))
        assertEquals("''quoted''", read("'''''quoted'''''"))
        assertEquals("a\"\"b\"\"\"c", read("\"\"\"a\"\"b\"\"\\\"c\"\"\""))
        assertFailsWith<TomlParseException> { read("'''x''''''") }
    }

    @Test
    fun lineContinuations() {
        assertEquals("The quick brown fox.", read("\"\"\"\nThe quick brown \\\n\n   fox.\"\"\""))
        assertEquals("one two", read("\"\"\"\\\n one \\  \t\r\n\t two\\\n\"\"\""))
        assertEquals("line\\\nnext", read("\"\"\"line\\\\\nnext\"\"\""))
        assertFailsWith<TomlParseException> { read("\"\"\"abc\\  x\"\"\"") }
    }

    @Test
    fun crlfIsNormalized() {
        assertEquals("a\nb", read("\"\"\"\r\na\r\nb\"\"\""))
        assertEquals("a\nb", read("'''\r\na\r\nb'''"))
        assertEquals("\r", read("\"\\r\""))
        assertFailsWith<TomlParseException> { read("'''a\rb'''") }
    }

    @Test
    fun invalidEscapesAndControls() {
        for (value in [
            "\"\\a\"", "\"\\/\"", "\"\\u123\"", "\"\\uXXXX\"", "\"\\U00110000\"",
            "\"\\uD800\"", "\"\\uDFFF\"", "\"\\xG0\"", "\"line\nnext\"", "'line\nnext'",
            "\"control\u0001\"", "'control\u007f'", "'''control\u000c'''", "\"unterminated", "'''unterminated",
        ]) assertFailsWith<TomlParseException>(value) { read(value) }
    }

    @Test
    fun unicodeValidityAndTabCharacters() {
        assertEquals("你好 🌍\t", read("\"你好 🌍\t\""))
        assertEquals("🌍", Toml.parse("'🌍'='🌍' #🌍").Object.getValue("🌍").String)
        for (source in ["x='\ud800'", "x='\udc00'", "#\ud800", "'\ud800'=1"])
            assertFailsWith<TomlParseException> { Toml.parse(source) }
    }
}