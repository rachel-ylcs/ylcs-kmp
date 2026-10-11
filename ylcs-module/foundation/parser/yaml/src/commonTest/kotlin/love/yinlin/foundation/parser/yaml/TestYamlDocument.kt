package love.yinlin.foundation.parser.yaml

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import kotlin.test.*

class TestYamlDocument {
    @Serializable
    private data class Configuration(val document: YamlDocument, val documents: List<YamlDocument>, val optional: YamlDocument? = null)

    @Test
    fun parseAndDslReturnDocuments() {
        val parsed: YamlDocument = Yaml.parse("name: demo\nserver:\n  port: 8080")
        val configured: YamlDocument = Yaml(YamlConfiguration(style = YamlStyle.Flow)).parse(parsed.toString())
        val built: YamlDocument = yaml {
            "name" with "demo"
            obj("server") { "port" with 8080 }
        }
        assertEquals(parsed, built)
        assertEquals(parsed, configured)
        assertEquals(8080, parsed.root.Object.obj("server")["port"].Int)
        assertEquals(parsed.root.hashCode(), parsed.hashCode())
        assertNotEquals(parsed.root, parsed)
        assertEquals(Yaml.encodeToString(parsed), parsed.toString())
        assertEquals(parsed, Yaml.parse(parsed.toString()))
    }

    @Test
    fun documentsPreserveEveryRootType() {
        val values: List<YamlElement> = [
            buildYamlObject { "key" with "value" }, buildYamlObject { },
            buildYamlArray { add(1); add(true); add(null) }, buildYamlArray { },
            YamlPrimitive("true"), YamlPrimitive("one\n two\n\n中"),
            YamlPrimitive(42), YamlPrimitive(false), YamlNull,
        ]
        for (style in YamlStyle.entries) {
            val yaml = Yaml(YamlConfiguration(style = style))
            for (root in values) {
                val document = YamlDocument(root)
                assertSame(root, document.root)
                assertEquals(yaml.encodeToString(root), yaml.encodeToString(document))
                assertEquals(document, yaml.parse(yaml.encodeToString(document)))
                assertEquals(root, yaml.parse(yaml.encodeToString(root)).root)
            }
        }
    }

    @Test
    fun emptyDocumentsAndDocumentExtensions() {
        for (source in ["", "# empty\n", "---\n", "---\n...\n"]) {
            val document: YamlDocument = Yaml.parse(source)
            assertSame(YamlNull, document.root)
            assertEquals(YamlDocument(YamlNull), document)
            assertEquals(document, Yaml.parse(document.toString()))
        }
        val document = yaml { }
        assertTrue(document.root.Object.isEmpty())
        val element: YamlElement = document
        assertSame(document, element.Document)
        assertSame(document, element.DocumentNull)
        assertNull(element.ObjectNull)
        assertNull(document.root.DocumentNull)
        val missing: YamlElement? = null
        assertNull(missing.DocumentNull)
        assertFailsWith<ClassCastException> { document.root.Document }
    }

    @Test
    fun documentsCannotBeNestedInValues() {
        val document = yaml { "key" with 1 }
        assertFailsWith<IllegalArgumentException> { YamlDocument(document) }
        val invalid: List<YamlElement> = [
            buildYamlArray { add(document) },
            buildYamlObject { "nested" with document },
            yaml { "nested" with document },
        ]
        for (style in YamlStyle.entries) {
            val yaml = Yaml(YamlConfiguration(style = style))
            for (value in invalid) assertFailsWith<IllegalArgumentException> { yaml.encodeToString(value) }
        }
    }

    @Test
    fun serializeDocumentsAsYamlStrings() {
        val documents = [
            Yaml.parse("""
                name: 银临茶舍
                escaped: "quote\" and slash\\ and newline\n"
                defaults: &defaults {port: 8080, enabled: true}
                server: {<<: *defaults, port: 8081}
                users: [Alice, null]
                description: |-
                  first line
                  second line
            """.trimIndent()),
            Yaml.parse("[1, true, null, {key: value}]"),
            Yaml.parse("\"true\""), Yaml.parse("42"), Yaml.parse("false"),
            Yaml.parse("null"), Yaml.parse(""), yaml { },
        ]
        assertEquals(PrimitiveKind.STRING, YamlDocument.serializer().descriptor.kind)
        for (document in documents) {
            val encoded = document.toJsonString()
            val json = assertIs<JsonPrimitive>(Json.parseToJsonElement(encoded))
            assertTrue(json.isString)
            assertEquals(Yaml.encodeToString(document), json.content)
            assertEquals(document, encoded.parseJsonValue<YamlDocument>())
        }
        val configuration = Configuration(documents.first(), documents)
        assertEquals(configuration, configuration.toJsonString().parseJsonValue<Configuration>())
    }

    @Test
    fun deserializeRequiresAValidSingleYamlDocumentString() {
        for (source in ["[1, 2", "a: 1\na: 2", "---\na: 1\n---\nb: 2"]) {
            assertFailsWith<SerializationException> { JsonPrimitive(source).toString().parseJsonValue<YamlDocument>() }
        }
        for (json in ["{}", "[]", "null"]) {
            assertFailsWith<SerializationException> { json.parseJsonValue<YamlDocument>() }
        }
    }
}
