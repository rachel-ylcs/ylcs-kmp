package love.yinlin.foundation.parser.yaml

import kotlin.test.*

class TestYamlLimits {
    @Test
    fun depthLimitsForParsingAndWriting() {
        val yaml = Yaml(YamlConfiguration(maxDepth = 2))
        assertEquals(1, yaml.parse("[[1]]").Array[0].Array[0].Int)
        assertFailsWith<YamlParseException> { yaml.parse("[[[1]]]") }
        assertFailsWith<YamlParseException> { yaml.parse("a:\n  b:\n    c:\n      d: 1") }
        val deep = Yaml.parse("[[[1]]]")
        assertFailsWith<IllegalArgumentException> { yaml.encodeToString(deep) }
        assertFailsWith<IllegalArgumentException> { Yaml(YamlConfiguration(style = YamlStyle.Flow, maxDepth = 2)).encodeToString(deep) }
    }

    @Test
    fun aliasLimitsAndSharedReferences() {
        val yaml = Yaml(YamlConfiguration(maxAliases = 1))
        val root = yaml.parse("a: &a [1, 2]\nb: *a") as YamlObject
        assertSame(root["a"], root["b"])
        assertFailsWith<YamlParseException> { yaml.parse("a: &a 1\nb: [*a, *a]") }
        assertFailsWith<YamlParseException> { Yaml(YamlConfiguration(maxAliases = 0)).parse("[&a 1, *a]") }
    }

    @Test
    fun nodeLimitsBoundAliasExpansionWhenWriting() {
        val yaml = Yaml(YamlConfiguration(maxNodes = 8))
        assertFailsWith<YamlParseException> { yaml.parse("[1, 2, 3, 4, 5, 6, 7, 8]") }
        val root = Yaml.parse("a: &a [1, 2, 3]\nb: [*a, *a, *a]")
        assertFailsWith<IllegalArgumentException> { yaml.encodeToString(root) }
    }

    @Test
    fun configurationValidation() {
        assertFailsWith<IllegalArgumentException> { YamlConfiguration(indent = 0) }
        assertFailsWith<IllegalArgumentException> { YamlConfiguration(indent = 10) }
        assertFailsWith<IllegalArgumentException> { YamlConfiguration(maxDepth = 0) }
        assertFailsWith<IllegalArgumentException> { YamlConfiguration(maxAliases = -1) }
        assertFailsWith<IllegalArgumentException> { YamlConfiguration(maxNodes = 0) }
    }
}
