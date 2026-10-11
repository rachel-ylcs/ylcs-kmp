package love.yinlin.foundation.parser.toml

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import kotlin.test.*

class TestTomlDocument {
    @Serializable
    private data class Configuration(val document: TomlDocument, val documents: List<TomlDocument>, val optional: TomlDocument? = null)

    @Test
    fun parseAndDslReturnDocuments() {
        val parsed: TomlDocument = Toml.parse("name = 'demo'\n[server]\nport = 8080")
        val configured: TomlDocument = Toml(TomlConfiguration(version = TomlVersion.V1_0)).parse(parsed.toString())
        val built: TomlDocument = toml {
            "name" with "demo"
            obj("server") { "port" with 8080 }
        }
        assertEquals(parsed, built)
        assertEquals(parsed, configured)
        assertEquals(8080, parsed.root.obj("server")["port"].Int)
        assertEquals(parsed.root.hashCode(), parsed.hashCode())
        assertNotEquals<TomlElement>(parsed.root, parsed)
        assertEquals(Toml.encodeToString(parsed), parsed.toString())
        assertEquals(parsed, Toml.parse(parsed.toString()))
    }

    @Test
    fun emptyDocumentsAndDocumentExtensions() {
        val empty = toml { }
        for (source in ["", " \t# empty\n"]) {
            val document: TomlDocument = Toml.parse(source)
            assertTrue(document.root.isEmpty())
            assertEquals(empty, document)
            assertEquals("", document.toString())
        }
        val element: TomlElement = empty
        assertSame(empty, element.Document)
        assertSame(empty, element.DocumentNull)
        assertNull(element.ObjectNull)
        assertNull(empty.root.DocumentNull)
        val missing: TomlElement? = null
        assertNull(missing.DocumentNull)
        assertFailsWith<ClassCastException> { empty.root.Document }
    }

    @Test
    fun elementsEncodeAsValueFragments() {
        val values: List<TomlElement> = [
            TomlPrimitive(42), TomlPrimitive(true), TomlPrimitive("quote\" and newline\n中"),
            TomlPrimitive.dateTime("2026-10-11T12:30:00+08:00"),
            buildTomlArray {
                add(1)
                obj { "key" with "value" }
            },
            buildTomlObject {
                "key" with 1
                obj("nested") {
                    "enabled" with true
                }
            },
        ]
        for (style in TomlStyle.entries) {
            val toml = Toml(TomlConfiguration(style = style))
            for (value in values) {
                val fragment = toml.encodeToString(value)
                assertEquals(value, toml.parse("value = $fragment").root["value"])
                assertEquals(Toml.encodeToString(value), value.toString())
            }
        }
        assertEquals("{key = 1}", Toml.encodeToString(buildTomlObject { "key" with 1 }))
        assertEquals("key = 1\n", Toml.encodeToString(toml { "key" with 1 }))
        assertEquals("[]", Toml.encodeToString(buildTomlArray { }))
    }

    @Test
    fun documentsCannotBeNestedInValues() {
        val document = toml { "key" with 1 }
        val invalid: List<TomlElement> = [
            buildTomlArray { add(document) },
            buildTomlObject { "nested" with document },
            toml { "nested" with document },
        ]
        for (style in TomlStyle.entries) {
            val toml = Toml(TomlConfiguration(style = style))
            for (value in invalid) assertFailsWith<IllegalArgumentException> { toml.encodeToString(value) }
        }
    }

    @Test
    fun serializeDocumentsAsTomlStrings() {
        val document = Toml.parse("""
            name = "银临茶舍"
            escaped = "quote\" and slash\\ and newline\n"
            created = 2026-10-11T12:30:00+08:00
            [server]
            enabled = true
            ports = [8080, 8081]
            [[users]]
            name = "Alice"
        """.trimIndent())
        assertEquals(PrimitiveKind.STRING, TomlDocument.serializer().descriptor.kind)
        for (value in [document, toml { }]) {
            val encoded = value.toJsonString()
            val json = assertIs<JsonPrimitive>(Json.parseToJsonElement(encoded))
            assertTrue(json.isString)
            assertEquals(Toml.encodeToString(value), json.content)
            assertEquals(value, encoded.parseJsonValue<TomlDocument>())
        }
        val configuration = Configuration(document, [document, toml { }])
        assertEquals(configuration, configuration.toJsonString().parseJsonValue<Configuration>())
    }

    @Test
    fun deserializeRequiresAValidTomlDocumentString() {
        for (source in ["count =", "[1, 2]", "{key = 1}"]) {
            assertFailsWith<SerializationException> { JsonPrimitive(source).toString().parseJsonValue<TomlDocument>() }
        }
        for (json in ["{}", "[]", "null"]) {
            assertFailsWith<SerializationException> { json.parseJsonValue<TomlDocument>() }
        }
    }
}
