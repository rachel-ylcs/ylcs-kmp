package love.yinlin.foundation.parser.toml

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestTomlWriter {
    @Test
    fun tablesAreEmittedAfterAssignments() {
        val document = toml {
            obj("first") { "inner" with 1 }
            "root" with "after object in insertion order"
            obj("last") {}
        }
        val text = Toml.encodeToString(document)
        assertTrue(text.indexOf("root =") < text.indexOf("[first]"))
        assertTrue(text.contains("[last]"))
        assertEquals(document, Toml.parse(text))
    }

    @Test
    fun inlineDocumentStyle() {
        val document = toml {
            obj("table") { "a" with 1 }
            arr("list") { obj { "b" with 2 } }
        }
        val codec = Toml(TomlConfiguration(style = TomlStyle.Inline, version = TomlVersion.V1_0))
        val text = codec.encodeToString(document)
        assertTrue(text.contains("table = {a = 1}"))
        assertFalse(text.contains("[[list]]"))
        assertEquals(document, codec.parse(text))
    }

    @Test
    fun nestedArraysOfTablesAndEmptyTables() {
        val document = toml {
            arr("parents") {
                obj {
                    obj("empty") {}
                    arr("children") { obj { "n" with 1 }; obj {} }
                }
                obj {
                    arr("children") { obj { "n" with 2 } }
                }
            }
            arr("empty") {}
            arr("mixed") { obj {}; add(1); arr {} }
        }
        assertEquals(document, Toml.parse(Toml.encodeToString(document)))
    }

    @Test
    fun quotedKeysAndStringEscapes() {
        val document = toml {
            "" with ""
            "a.b" with "literal dotted key"
            "你好\n\"" with "quote\" slash\\ newline\n tab\t backspace\b CR\r FF\u000c ESC\u001b DEL\u007f NUL\u0000 🌍"
            obj("table.[]") { obj("") { "#key" with "#value" } }
            arr("list.[]") { obj { "=" with true } }
        }
        val strict = Toml(TomlConfiguration(version = TomlVersion.V1_0))
        assertEquals(document, strict.parse(strict.encodeToString(document)))
        assertEquals(document, Toml.parse("value = $document").Object["value"])
    }

    @Test
    fun numericAndDateTextIsPreserved() {
        val document = Toml.parse("a=0xDEAD_beef\nb=+1.234567890123456789\nc=-nan\nd=1979-05-27t07:32:00.123456789z").Object
        val text = Toml.encodeToString(document)
        assertTrue(text.contains("0xDEAD_beef"))
        assertTrue(text.contains("+1.234567890123456789"))
        assertTrue(text.contains("1979-05-27t07:32:00.123456789z"))
        assertEquals(document, Toml.parse(text))
    }

    @Test
    fun documentRootAndInvalidUnicodeAreRejected() {
        assertFailsWith<IllegalArgumentException> { Toml.encodeToString(TomlPrimitive(1)) }
        assertFailsWith<IllegalArgumentException> { Toml.encodeToString(buildTomlArray {}) }
        assertFailsWith<IllegalArgumentException> { Toml.encodeToString(toml { "value" with "\ud800" }) }
        assertFailsWith<IllegalArgumentException> { Toml.encodeToString(toml { "\udc00" with 1 }) }
        assertFailsWith<ClassCastException> { TomlPrimitive(true).Array }
    }


    @Test
    fun deterministicRandomRoundTrips() {
        val random = Random(7419)
        val keys = ["", "plain", "a.b", "中文", "quote\"", "line\n", "123", "[]"]
        fun element(depth: Int): TomlElement {
            return when (random.nextInt(if (depth == 0) 4 else 6)) {
                0 -> TomlPrimitive(["", "true", "a\nb", "🌍", "\u0000\u007f", "\"\\", "中文"].random(random))
                1 -> TomlPrimitive(random.nextLong())
                2 -> TomlPrimitive(random.nextBoolean())
                3 -> TomlPrimitive(random.nextDouble())
                4 -> buildTomlArray { repeat(random.nextInt(4)) { add(element(depth - 1)) } }
                else -> buildTomlObject { repeat(random.nextInt(4)) { keys.random(random) with element(depth - 1) } }
            }
        }
        val inline = Toml(TomlConfiguration(style = TomlStyle.Inline, version = TomlVersion.V1_0))
        repeat(400) {
            val document = toml { repeat(random.nextInt(7)) { keys.random(random) with element(4) } }
            assertEquals(document, Toml.parse(Toml.encodeToString(document)))
            assertEquals(document, inline.parse(inline.encodeToString(document)))
            assertEquals(document, Toml.parse("value = $document").Object["value"])
        }
    }

    @Test
    fun largeConfiguration() {
        val source = buildString {
            append("title='services'\n")
            repeat(2_000) { index ->
                append("[[services]]\nname='service-").append(index).append("'\nport=").append(8000 + index)
                append("\n[services.options]\nenabled=true\n")
            }
        }
        val document = Toml.parse(source).Object
        assertEquals(2_000, document.arr("services").size)
        assertEquals(9999, document.arr("services").last().Object.getValue("port").Int)
        assertEquals(document, Toml.parse(Toml.encodeToString(document)))
    }
}