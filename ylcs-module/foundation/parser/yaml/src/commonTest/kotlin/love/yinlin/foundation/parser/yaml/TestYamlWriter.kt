package love.yinlin.foundation.parser.yaml

import kotlin.random.Random
import kotlin.test.*

class TestYamlWriter {
    @Test
    fun blockAndFlowRoundTrips() {
        val value = yaml {
            "name" with "银临茶舍"
            "enabled" with true
            "nil" with null
            obj("server") {
                "host" with "localhost"
                "port" with 8080
            }
            arr("users") {
                obj {
                    "name" with "Alice"
                    arr("roles") {
                        add("admin")
                        add("reader")
                    }
                }
                add("guest")
                add(null)
                arr { }
                obj { }
            }
        }
        for (style in YamlStyle.entries) {
            val yaml = Yaml(YamlConfiguration(style = style))
            assertEquals(value, yaml.parse(yaml.encodeToString(value)))
        }
        assertTrue(Yaml.encodeToString(value).contains("  port: 8080\n"))
    }

    @Test
    fun quoteAmbiguousAndReservedStringsAndKeys() {
        val strings = ["", "true", "FALSE", "null", "~", "012", "0xff", ".nan", "1e2", "---", "...", "--- text",
            " leading", "trailing ", "a: b", "a # b", "#hash", "[array]", "{map}", "a,b", "@value", "`value", "a:\tb",
            "<<", "it\"s", "back\\slash", "\u0000\u0001\u001f", "\u0085", "😀中", "a:b", "a#b", "Bob's \"title\"", "\ufeff", "\ufefftext"]
        val value = buildYamlObject {
            for (text in strings) text with text
        }
        for (style in YamlStyle.entries) {
            val yaml = Yaml(YamlConfiguration(style = style))
            assertEquals(value, yaml.parse(yaml.encodeToString(value)))
            assertEquals(YamlPrimitive("\ufefftext"), yaml.parse(yaml.encodeToString(YamlPrimitive("\ufefftext"))))
        }
    }

    @Test
    fun multilineStringsPreserveLeadingSpacesAndAllTrailingNewlines() {
        val strings = ["one\ntwo", "one\ntwo\n", "one\ntwo\n\n", " one\n  two", "\ntext\n", "one\n\nthree",
            "\n", "\n\n", " \n  \n", "one\n\ttext", "one\r\ntwo", "#first\n---\n...", "first\n  \nlast"]
        for (indent in [1, 2, 4, 9]) {
            for (literal in [false, true]) {
                val yaml = Yaml(YamlConfiguration(indent = indent, blockStrings = literal))
                for (text in strings) {
                    val value = YamlPrimitive(text)
                    assertEquals(value, yaml.parse(yaml.encodeToString(value)), "indent=$indent, literal=$literal, text=$value")
                    val mapping = buildYamlObject {
                        "text" with text
                        "next" with "end"
                    }
                    assertEquals(mapping, yaml.parse(yaml.encodeToString(mapping)))
                }
            }
        }
    }

    @Test
    fun numericConstructorsAndSpecialFloats() {
        val value = buildYamlArray {
            add(Long.MIN_VALUE)
            add(Long.MAX_VALUE)
            add(-0.0)
            add(0.25f)
            add(Double.NaN)
            add(Double.POSITIVE_INFINITY)
            add(Float.NEGATIVE_INFINITY)
        }
        for (style in YamlStyle.entries) {
            val yaml = Yaml(YamlConfiguration(style = style))
            assertEquals(value, yaml.parse(yaml.encodeToString(value)))
        }
    }

    @Test
    fun deterministicGeneratedTreesRoundTrip() {
        val random = Random(12345)
        val special = ["true", "#comment", "a: b", " leading\n  next\n\n", "😀", "", "a,b", "\\\"", "...", "\u0000"]
        fun tree(depth: Int): YamlElement {
            return when (random.nextInt(if (depth == 0) 4 else 6)) {
                0 -> YamlNull
                1 -> YamlPrimitive(random.nextBoolean())
                2 -> YamlPrimitive(random.nextLong())
                3 -> YamlPrimitive(special[random.nextInt(special.size)])
                4 -> buildYamlArray { repeat(random.nextInt(5)) { add(tree(depth - 1)) } }
                else -> buildYamlObject {
                    repeat(random.nextInt(5)) {
                        "${special[random.nextInt(special.size)]}$it" with tree(depth - 1)
                    }
                }
            }
        }
        for (style in YamlStyle.entries) {
            val yaml = Yaml(YamlConfiguration(style = style))
            repeat(200) {
                val value = tree(4)
                assertEquals(value, yaml.parse(yaml.encodeToString(value)))
            }
        }
    }

    @Test
    fun largeConfigurationDocument() {
        val value = buildYamlObject {
            repeat(2_000) { i ->
                obj("service$i") {
                    "port" with (8000 + i)
                    "host" with "https://example.com/service/$i"
                    arr("tags") { add("production"); add("service$i") }
                }
            }
        }
        assertEquals(value, Yaml.parse(Yaml.encodeToString(value)))
    }
}
