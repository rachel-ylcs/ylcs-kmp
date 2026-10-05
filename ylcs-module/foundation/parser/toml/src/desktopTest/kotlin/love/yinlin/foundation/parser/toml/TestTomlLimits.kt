package love.yinlin.foundation.parser.toml

import kotlin.test.*

class TestTomlLimits {
    @Test
    fun depthAndNodeLimits() {
        val limited = Toml(TomlConfiguration(maxDepth = 3, maxNodes = 5))
        assertEquals(1, limited.parse("a.b=1").Object.obj("a").getValue("b").Int)
        for (source in ["a.b.c.d=1", "x=[[[1]]]", "x=[1,2,3,4]", "[[a.b.c]]"])
            assertFailsWith<TomlParseException>(source) { limited.parse(source) }
        assertFailsWith<IllegalArgumentException> { TomlConfiguration(maxDepth = 0) }
        assertFailsWith<IllegalArgumentException> { TomlConfiguration(maxDepth = 513) }
        assertFailsWith<IllegalArgumentException> { TomlConfiguration(maxNodes = 0) }
    }

    @Test
    fun writerLimitsMatchParserLimits() {
        val limited = Toml(TomlConfiguration(maxDepth = 3, maxNodes = 5))
        val small = toml { obj("a") { "b" with 1 } }
        assertEquals(small, limited.parse(limited.encodeToString(small)))
        assertFailsWith<IllegalArgumentException> { limited.encodeToString(toml { arr("a") { repeat(4) { add(it) } } }) }
        assertFailsWith<IllegalArgumentException> { limited.encodeToString(toml { obj("a") { obj("b") { obj("c") { "d" with 1 } } } }) }
    }
}