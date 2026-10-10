package love.yinlin.foundation.parser.toml

import kotlin.test.*

class TestTomlParser {
    private fun mapping(source: String): TomlObject = Toml.parse(source).Object
    private val strict = Toml(TomlConfiguration(version = TomlVersion.V1_0))

    @Test
    fun emptyDocuments() {
        assertTrue(mapping("").isEmpty())
        assertTrue(mapping(" \t# 配置\r\n\n").isEmpty())
    }

    @Test
    fun ordinaryConfiguration() {
        val value = mapping("""
            title = "TOML Example"
            [owner]
            name = "Tom"
            dob = 1979-05-27T07:32:00-08:00
            [database]
            enabled = true
            ports = [8000, 8001, 8002]
            data = [["delta", "phi"], [3.14]]
            temp_targets = { cpu = 79.5, case = 72.0 }
        """.trimIndent())
        assertEquals("TOML Example", value.getValue("title").String)
        assertEquals("Tom", value.obj("owner").getValue("name").String)
        assertEquals(TomlScalarKind.OffsetDateTime, value.obj("owner").getValue("dob").let { (it as TomlPrimitive).kind })
        assertTrue(value.obj("database").getValue("enabled").Boolean)
        assertEquals(8001, value.obj("database").arr("ports")[1].Int)
        assertEquals(79.5, value.obj("database").obj("temp_targets").getValue("cpu").Double)
    }

    @Test
    fun quotedAndDottedKeys() {
        val value = mapping("""
            "" = 'empty'
            3.14159 = "pi"
            site . "google.com" = true
            physical.color = "orange"
            physical.shape = "round"
            "\u0061" = 1
        """.trimIndent())
        assertEquals("empty", value.getValue("").String)
        assertEquals("pi", value.obj("3").getValue("14159").String)
        assertTrue(value.obj("site").getValue("google.com").Boolean)
        assertEquals(setOf("color", "shape"), value.obj("physical").keys)
        assertEquals(1, value.getValue("a").Int)
    }

    @Test
    fun implicitParentsMayBeDefinedLater() {
        val value = mapping("[a.b.c]\nx = 1\n[a]\ny = 2\n[a.b]\nz = 3")
        assertEquals(1, value.obj("a").obj("b").obj("c").getValue("x").Int)
        assertEquals(2, value.obj("a").getValue("y").Int)
        assertEquals(3, value.obj("a").obj("b").getValue("z").Int)
    }

    @Test
    fun dottedTablesCanHaveHeaderSubtables() {
        val value = mapping("fruit.apple.color = 'red'\n[fruit.apple.texture]\nsmooth = true")
        assertTrue(value.obj("fruit").obj("apple").obj("texture").getValue("smooth").Boolean)
    }

    @Test
    fun dottedKeysCanDefineAnImplicitParent() {
        val value = mapping("[a.b.c]\nx = 1\n[a]\nb.y = 2")
        assertEquals(2, value.obj("a").obj("b").getValue("y").Int)
    }

    @Test
    fun nestedArraysOfTablesUseTheLatestParent() {
        val value = mapping("""
            [[fruits]]
            name = "apple"
            [fruits.physical]
            color = "red"
            [[fruits.varieties]]
            name = "red delicious"
            [[fruits.varieties]]
            name = "granny smith"
            [[fruits]]
            name = "banana"
            [[fruits.varieties]]
            name = "plantain"
        """.trimIndent())
        val fruits = value.arr("fruits")
        assertEquals(2, fruits.size)
        assertEquals("red", fruits[0].Object.obj("physical").getValue("color").String)
        assertEquals(2, fruits[0].Object.arr("varieties").size)
        assertEquals("plantain", fruits[1].Object.arr("varieties")[0].Object.getValue("name").String)
        assertFalse(fruits[1].Object.containsKey("physical"))
    }

    @Test
    fun mixedArraysAndComments() {
        val value = mapping("x = [1, # integer\n 'two', true, [3.0], {a=4},]\n").arr("x")
        assertEquals(5, value.size)
        assertEquals(4, value[4].Object.getValue("a").Int)
    }

    @Test
    fun inlineDottedTables() {
        val value = mapping("x = {a.b=1, a.c=2, d={e=3}}").obj("x")
        assertEquals(setOf("b", "c"), value.obj("a").keys)
        assertEquals(3, value.obj("d").getValue("e").Int)
    }

    @Test
    fun duplicateKeysAreRejected() {
        for (source in ["a=1\na=2", "a=1\n'a'=2", "a=1\n\"\\u0061\"=2", "x={a=1,a=2}", "a.b=1\na.b=2"])
            assertFailsWith<TomlParseException>(source) { mapping(source) }
    }

    @Test
    fun tableRedefinitionsAreRejected() {
        for (source in [
            "[a]\n[a]", "a.b=1\n[a]", "[a]\nb.c=1\n[a.b]", "a=1\n[a]",
            "[a.b]\nx=1\n[a]\nb.y=2", "a.b=1\na=2", "a=1\na.b=2",
            "[a.b.c]\nx=1\n[a]\nb.y=2\n[a.b]",
        ]) assertFailsWith<TomlParseException>(source) { mapping(source) }
    }

    @Test
    fun inlineTablesAreSealed() {
        for (source in ["x={a=1}\nx.b=2", "x={a=1}\n[x.b]", "x={a.b=1}\n[x.a]", "x={a={b=1},a.c=2}"])
            assertFailsWith<TomlParseException>(source) { mapping(source) }
    }

    @Test
    fun arraysCannotBeRedefinedAsTables() {
        for (source in [
            "a=[]\n[[a]]", "a=[{x=1}]\n[[a]]", "[[a]]\n[a]", "[a]\n[[a]]",
            "[a.b]\nx=1\n[[a]]", "[[a.b]]\n[a]\nb.y=2", "a=[]\n[a.b]",
        ]) assertFailsWith<TomlParseException>(source) { mapping(source) }
    }

    @Test
    fun missingValuesSeparatorsAndInvalidKeys() {
        for (source in [
            "a=", "a=#none", "a=null", "a=TRUE", "a=hello", "a=1 b=2", "a=[1 2]",
            "a=[,]", "a=[1,,2]", "a={x=1 y=2}", "a={,}", "a. =1", "=1", "é=1",
            "[a\n]", "[a] x=1", "[[]]", "a\n=1", "a=\n1", "\"\"\"a\"\"\"=1",
        ]) assertFailsWith<TomlParseException>(source) { mapping(source) }
    }

    @Test
    fun inlineTableVersionRules() {
        val source = "x={\n a=1, # comment\n b=2,\n}"
        assertEquals(2, mapping(source).obj("x").size)
        for (text in [source, "x={a=1,}", "x={\n}", "x={a=1 # comment\n}"])
            assertFailsWith<TomlParseException> { strict.parse(text) }
        assertEquals(1, strict.parse("x={a=[\n1, # allowed inside value\n]}").Object.obj("x").arr("a").size)
    }

    @Test
    fun invalidLineEndingsAndComments() {
        for (source in ["a=1\rb=2", "#bad\u0001", "a=1 #bad\u007f", "#bad\r", "a=1\u00a0"])
            assertFailsWith<TomlParseException>(source) { mapping(source) }
    }

    @Test
    fun parsingRequiresACompleteDocument() {
        assertEquals(2, mapping("value = [1, 2] #comment")["value"].Array.size)
        assertEquals(1, mapping("value = {a=1}")["value"].Object["a"].Int)
        assertEquals("hello", mapping("value = 'hello'")["value"].String)
        for (source in ["1", "'hello'", "[1, 2]", "{a=1}", "1 2"])
            assertFailsWith<TomlParseException>(source) { mapping(source) }
    }

    @Test
    fun errorsHaveSourceLocations() {
        val error = assertFailsWith<TomlParseException> { mapping("a=1\r\nb = nope") }
        assertEquals(2, error.line)
        assertEquals(5, error.column)
        assertEquals(9, error.offset)
        assertTrue(error.message.orEmpty().contains("line 2, column 5"))
    }
}