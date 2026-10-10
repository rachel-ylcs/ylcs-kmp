package love.yinlin.foundation.parser.toml

import kotlin.test.*

class TestTomlDsl {
    @Test
    fun dslAndNodeAccess() {
        val document = toml {
            "name" with "ylcs"
            "version" with 1
            "created" with TomlPrimitive.dateTime("2026-10-05")
            obj("server") {
                "enabled" with true
                "port" with 8080
            }
            arr("products") {
                obj { "name" with "one" }
                obj { "name" with "two" }
            }
        }
        assertEquals(8080, document.obj("server").getValue("port").Int)
        assertEquals("two", document.arr("products")[1].Object.getValue("name").String)
        assertEquals(document, Toml.parse(Toml.encodeToString(document)))
        assertTrue(Toml.encodeToString(document).contains("[[products]]"))
        assertEquals(document, Toml.parse("value = $document").Object["value"])
    }

    @Test
    fun inputCollectionsAndBuildersAreSnapshots() {
        val map = linkedMapOf<String, TomlElement>("a" to TomlPrimitive(1))
        val obj = TomlObject(map)
        map["a"] = TomlPrimitive(2)
        assertEquals(1, obj.getValue("a").Int)
        val list: MutableList<TomlElement> = [TomlPrimitive(1)]
        val arr = TomlArray(list)
        list.clear()
        assertEquals(1, arr.size)
        lateinit var objectBuilder: TomlObjectBuilder
        lateinit var arrayBuilder: TomlArrayBuilder
        val built = toml {
            objectBuilder = this
            arr("a") { arrayBuilder = this; add(1) }
        }
        objectBuilder.apply { "later" with true }
        arrayBuilder.add(2)
        assertFalse(built.containsKey("later"))
        assertEquals(1, built.arr("a").size)
    }

    @Test
    fun collectionEqualityAndMerge() {
        val one = toml { "a" with 1; arr("b") { add(true) } }
        val two = buildTomlObject { merge(one) }
        assertEquals(one, two)
        assertEquals(one.hashCode(), two.hashCode())
        assertEquals(buildTomlArray { add(1); add(2) }, buildTomlArray { merge(buildTomlArray { add(1); add(2) }) })
        assertNotEquals(TomlPrimitive("1"), TomlPrimitive(1))
        assertNotEquals(one, toml { "a" with 2 })
    }

    @Test
    fun scalarExtensionsReadNullableMembers() {
        val root = toml {
            "name" with "demo"
            "enabled" with true
            "count" with 42L
            "ratio" with 0.25f
            "digits" with "123"
            "created" with TomlPrimitive.dateTime("2026-10-05")
        }
        val element: TomlElement = root
        assertEquals(root, element.Object)
        assertEquals("demo", root["name"].String)
        assertEquals(true, root["enabled"].Boolean)
        assertEquals(true, root["enabled"].BooleanNull)
        assertEquals(42, root["count"].Int)
        assertEquals(42, root["count"].IntNull)
        assertEquals(42L, root["count"].Long)
        assertEquals(42L, root["count"].LongNull)
        assertEquals(0.25f, root["ratio"].Float)
        assertEquals(0.25f, root["ratio"].FloatNull)
        assertEquals(0.25, root["ratio"].Double)
        assertEquals(0.25, root["ratio"].DoubleNull)
        assertEquals(123, root["digits"].Int)
        assertEquals("42", root["count"].StringNull)
        assertEquals("2026-10-05", root["created"].String)
        assertEquals(root, Toml.parse(Toml.encodeToString(root)))
    }

    @Test
    fun nullableScalarExtensionsHandleMissingAndCollectionNodes() {
        val root = toml {
            "text" with "not a number"
            obj("table") { }
            arr("items") { }
        }
        for (node in [root["missing"], root["table"], root["items"]]) {
            assertNull(node.BooleanNull)
            assertNull(node.IntNull)
            assertNull(node.LongNull)
            assertNull(node.FloatNull)
            assertNull(node.DoubleNull)
            assertNull(node.StringNull)
        }
        assertNull(root["text"].BooleanNull)
        assertNull(root["text"].IntNull)
        assertNull(root["text"].LongNull)
        assertNull(root["text"].FloatNull)
        assertNull(root["text"].DoubleNull)
        assertEquals("not a number", root["text"].StringNull)
    }

    @Test
    fun collectionExtensionsProvideOptionalAndEmptyAccess() {
        val table = toml { "x" with 1 }
        val array = buildTomlArray { add("first") }
        val root = toml {
            "table" with table
            "items" with array
            "scalar" with true
        }
        assertSame(table, root["table"].Object)
        assertSame(table, root["table"].ObjectNull)
        assertSame(table, root["table"].ObjectEmpty)
        assertSame(array, root["items"].Array)
        assertSame(array, root["items"].ArrayNull)
        assertSame(array, root["items"].ArrayEmpty)
        assertNull(root["items"].ObjectNull)
        assertNull(root["table"].ArrayNull)
        assertNull(root["missing"].ObjectNull)
        assertNull(root["missing"].ArrayNull)
        assertTrue(root["missing"].ObjectEmpty.isEmpty())
        assertTrue(root["missing"].ArrayEmpty.isEmpty())
        assertTrue(root["scalar"].ObjectEmpty.isEmpty())
        assertTrue(root["scalar"].ArrayEmpty.isEmpty())
    }

    @Test
    fun strictExtensionsRejectMissingOrWrongValues() {
        val missing: TomlElement? = null
        assertFailsWith<NullPointerException> { missing.String }
        assertFailsWith<NullPointerException> { missing.Int }
        assertFailsWith<NullPointerException> { missing.Object }
        assertFailsWith<NullPointerException> { missing.Array }
        assertFailsWith<NullPointerException> { TomlPrimitive("invalid").Boolean }
        assertFailsWith<NullPointerException> { TomlPrimitive("invalid").Long }
        assertFailsWith<ClassCastException> { TomlPrimitive(1).Object }
        assertFailsWith<ClassCastException> { TomlPrimitive(1).Array }
        assertFailsWith<ClassCastException> { buildTomlArray { }.String }
        assertFailsWith<ClassCastException> { buildTomlObject { }.Double }
    }
}