package love.yinlin.foundation.parser.yaml

import kotlin.test.*

class TestYamlParser {
    @Test
    fun nestedBlockCollections() {
        val root = Yaml.parse("""
            app:
              name: 银临茶舍
              enabled: true
              ports: [8080, 8081]
            users:
              - name: Alice
                roles:
                  - admin
                  - reader
              - name: Bob
                roles: []
        """).Object
        assertEquals("银临茶舍", root.obj("app")["name"].String)
        assertTrue(root.obj("app")["enabled"].Boolean)
        assertEquals(8081, root.obj("app").arr("ports")[1].Int)
        assertEquals("reader", root.arr("users")[0].Object.arr("roles")[1].String)
        assertTrue(root.arr("users")[1].Object.arr("roles").isEmpty())
        assertEquals(["app", "users"], root.keys.toList())
    }

    @Test
    fun indentlessAndCompactSequences() {
        val root = Yaml.parse("items:\n- - one\n  - two\n- three\nnext: done\n").Object
        assertEquals("two", root.arr("items")[0].Array[1].String)
        assertEquals("done", root["next"].String)
    }

    @Test
    fun flowCollectionsAndJsonSyntax() {
        val root = Yaml.parse("""{"name":"demo","items":[1,true,null,{"x":2}],"empty":{}}""").Object
        assertEquals(2, root.arr("items")[3].Object["x"].Int)
        assertSame(YamlNull, root.arr("items")[2])
        assertEquals(root, Yaml.parse(root.toString()))
        assertEquals(2, Yaml.parse("[a: 1, b: 2,]").Array[1].Object["b"].Int)
        assertSame(YamlNull, Yaml.parse("{a, b: ,}").Object["a"])
    }

    @Test
    fun multilineFlowWithComments() {
        val root = Yaml.parse("""
            values: [
              one, # comment
              {two: 2, three: [3, 4,]},
              ]
            next: ok
        """).Object
        assertEquals(4, root.arr("values")[1].Object.arr("three")[1].Int)
        assertEquals("ok", root["next"].String)
        assertEquals("one two", Yaml.parse("[one\n  two]").Array[0].String)
    }

    @Test
    fun yaml12ScalarResolutionAndPrecision() {
        assertTrue(Yaml.parse("TRUE").Boolean)
        assertFalse(Yaml.parse("False").Boolean)
        for (text in ["~", "null", "Null", "NULL", ""]) assertSame(YamlNull, Yaml.parse(text))
        for (text in ["yes", "no", "on", "off", "2026-10-05", "1_000", "0b10"]) {
            assertTrue((Yaml.parse(text) as YamlPrimitive).isString, text)
        }
        assertEquals(12, Yaml.parse("012").Int)
        assertEquals(255, Yaml.parse("0xff").Int)
        assertEquals(-63, Yaml.parse("-0o77").Int)
        assertEquals(Long.MIN_VALUE, Yaml.parse("-9223372036854775808").Long)
        val huge = Yaml.parse("123456789012345678901234567890")
        assertFalse((huge as YamlPrimitive).isString)
        assertNull(huge.LongNull)
        assertEquals("123456789012345678901234567890", huge.content)
        assertEquals(huge, Yaml.parse(Yaml.encodeToString(huge)))
        val precise = Yaml.parse("1.234567890123456789e-10")
        assertEquals("1.234567890123456789e-10", precise.String)
        assertEquals(precise, Yaml.parse(Yaml.encodeToString(precise)))
        assertEquals(0.5, Yaml.parse(".5").Double)
        assertEquals(Double.POSITIVE_INFINITY, Yaml.parse("+.INF").Double)
        assertTrue(Yaml.parse(".NaN").Double.isNaN())
        assertFailsWith<Throwable> { huge.Long }
    }

    @Test
    fun quotedStringsAndUnicodeEscapes() {
        assertEquals("it's \\ literal", Yaml.parse("'it''s \\ literal'").String)
        assertTrue((Yaml.parse("\"true\"") as YamlPrimitive).isString)
        assertEquals("中😀\n\t\u0000", Yaml.parse("\"\\u4e2d\\U0001f600\\n\\t\\0\"").String)
        assertEquals("A B\u00a0\u2028\u2029\u0085", Yaml.parse("\"\\x41\\ B\\_\\L\\P\\N\"").String)
        assertFailsWith<YamlParseException> { Yaml.parse("\"\\uD800\"") }
        assertFailsWith<YamlParseException> { Yaml.parse("\"\\U00110000\"") }
        assertFailsWith<YamlParseException> { Yaml.parse("\"\\q\"") }
    }

    @Test
    fun multilineQuotedAndPlainStrings() {
        assertEquals("one two", Yaml.parse("value: \"one\n  two\"").Object["value"].String)
        assertEquals("one\ntwo", Yaml.parse("value: 'one\n\n  two'").Object["value"].String)
        assertEquals("onetwo", Yaml.parse("value: \"one\\\n  two\"").Object["value"].String)
        assertEquals("one two\nthree", Yaml.parse("value: one\n  two\n\n  three\nnext: end").Object["value"].String)
    }

    @Test
    fun commentsAndColonsInPlainScalars() {
        val root = Yaml.parse("""
            # header
            url: https://example.com/a#fragment # actual comment
            title: Bob's "quoted" title
            key: value:without-space
            quoted: "value # content" # comment
        """).Object
        assertEquals("https://example.com/a#fragment", root["url"].String)
        assertEquals("Bob's \"quoted\" title", root["title"].String)
        assertEquals("value:without-space", root["key"].String)
        assertEquals("value # content", root["quoted"].String)
        assertFailsWith<YamlParseException> { Yaml.parse("key: \"value\"#comment") }
    }

    @Test
    fun nullAndEmptyCollections() {
        val root = Yaml.parse("first:\nsecond: # empty\nlist:\n  -\n  - null\nmap: {}\nempty: []\n").Object
        assertSame(YamlNull, root["first"])
        assertSame(YamlNull, root["second"])
        assertSame(YamlNull, root.arr("list")[0])
        assertTrue(root.obj("map").isEmpty())
        assertTrue(root.arr("empty").isEmpty())
        assertNull(root["absent"])
        assertSame(YamlNull, Yaml.parse("# only comments\n"))
    }

    @Test
    fun literalScalarsAndChomping() {
        val root = Yaml.parse("strip: |-\n  one\n  two\n\nclip: |\n  one\n\nkeep: |+\n  one\n\nnext: value\n").Object
        assertEquals("one\ntwo", root["strip"].String)
        assertEquals("one\n", root["clip"].String)
        assertEquals("one\n\n", root["keep"].String)
        assertEquals("one", Yaml.parse("|-\n  one").String)
        assertEquals("one", Yaml.parse("|\n  one").String)
        assertEquals("", Yaml.parse("empty: |\n\nnext: ok").Object["empty"].String)
        assertEquals("\n", Yaml.parse("- |+\n   ").Array[0].String)
        assertEquals("x\n \n", Yaml.parse("foo: |\n  x\n   ").Object["foo"].String)
    }

    @Test
    fun foldedScalarsParagraphsAndMoreIndentedText() {
        val root = Yaml.parse("""
            text: >-
              one
              two

              three
                code
              four
            next: end
        """).Object
        assertEquals("one two\nthree\n  code\nfour", root["text"].String)
        assertEquals("one\n\ntwo", Yaml.parse(">-\n  one\n\n\n  two\n").String)
    }

    @Test
    fun blockScalarExplicitIndentAndSpecialContent() {
        val root = Yaml.parse("a: |2- # header\n   leading\n  # content\n  ---\n  \ttext\nnext: ok").Object
        assertEquals(" leading\n# content\n---\n\ttext", root["a"].String)
        assertEquals("  leading\n", Yaml.parse("|2\n    leading\n").String)
        assertFailsWith<YamlParseException> { Yaml.parse("a: |2\n b\n") }
        assertFailsWith<YamlParseException> { Yaml.parse("a: |0\n  text") }
    }

    @Test
    fun anchorsAliasesAndMergePrecedence() {
        val root = Yaml.parse("""
            base: &base
              host: localhost
              port: 80
            extra: &extra {port: 90, secure: true}
            copy: *base
            server:
              port: 443
              <<: [*base, *extra]
        """).Object
        assertSame(root["base"], root["copy"])
        assertEquals("localhost", root.obj("server")["host"].String)
        assertEquals(443, root.obj("server")["port"].Int)
        assertTrue(root.obj("server")["secure"].Boolean)
        val merged = Yaml.parse("{base: &b {n: 1}, copy: {<<: *b, n: 2}}").Object
        assertEquals(2, merged.obj("copy")["n"].Int)
        assertEquals(root, Yaml.parse(Yaml.encodeToString(root)))
    }

    @Test
    fun anchorRebindingUsesMostRecentDefinition() {
        val root = Yaml.parse("a: &n 1\nb: *n\nc: &n 2\nd: *n").Object
        assertSame(root["a"], root["b"])
        assertSame(root["c"], root["d"])
        assertEquals(2, root["d"].Int)
        val flow = Yaml.parse("- &mapping {key: value}\n- *mapping").Array
        assertSame(flow[0], flow[1])
    }

    @Test
    fun standardTags() {
        val root = Yaml.parse("""
            string: !!str 12
            tilde: !!str ~
            boolean: !!bool "True"
            integer: !!int "0xff"
            float: !!float 12
            sequence: !!seq [1, 2]
            mapping: !!map {name: value}
            longTag: !<tag:yaml.org,2002:str> true
        """).Object
        assertTrue((root["string"] as YamlPrimitive).isString)
        assertEquals("~", root["tilde"].String)
        assertTrue(root["boolean"].Boolean)
        assertEquals(255, root["integer"].Int)
        assertEquals("12.0", root["float"].String)
        assertTrue((root["longTag"] as YamlPrimitive).isString)
        assertEquals(root, Yaml.parse(Yaml.encodeToString(root)))
        assertFailsWith<YamlParseException> { Yaml.parse("!!bool yes") }
        assertFailsWith<YamlParseException> { Yaml.parse("!custom value") }
    }

    @Test
    fun singleDocumentMarkersAndDirectives() {
        val root = Yaml.parse("%YAML 1.2\n---\na: 1\n... # end\n# trailing comment\n").Object
        assertEquals(1, root["a"].Int)
        assertEquals(root, Yaml.parse(Yaml.encodeToString(root)))
        assertEquals("two", Yaml.parse("--- [two, 3]\n...\n").Array[0].String)
        assertEquals("hello", Yaml.parse("--- hello\n...\n").String)
        assertSame(YamlNull, Yaml.parse(""))
        assertSame(YamlNull, Yaml.parse("---\n"))
        assertSame(YamlNull, Yaml.parse("---\n...\n"))
        for (source in [
            "---\na: 1\n---\nb: 2", "---\na: 1\n...\n---\nb: 2",
            "---\n---\n", "---\n...\n---\n...\n", "---\na: &n 1\n---\nb: *n",
            "a: 1\n...\n%YAML 1.2\n---\nb: 2", "a: 1\n...\nb: 2", "a: 1\n... extra",
        ]) assertFailsWith<YamlParseException>(source) { Yaml.parse(source) }
        assertFailsWith<YamlParseException> { Yaml.parse("%YAML 1.1\n---\na: 1") }
        assertFailsWith<YamlParseException> { Yaml.parse("%YAML 1.2\na: 1") }
        assertFailsWith<YamlParseException> { Yaml.parse("%YAML 1.2\n%YAML 1.2\n---\na: 1") }
    }

    @Test
    fun codecCallsHaveIndependentAnchors() {
        val yaml = Yaml()
        val root = yaml.parse("a: &value [1, 2]\nb: *value").Object
        assertSame(root["a"], root["b"])
        assertFailsWith<YamlParseException> { yaml.parse("a: *value") }
        assertEquals(3, yaml.parse("a: &value 3\nb: *value").Object["b"].Int)
    }

    @Test
    fun bomAndAllCommonLineEndings() {
        for (separator in ["\n", "\r\n", "\r"]) {
            val root = Yaml.parse("\ufeffa: 1${separator}b: |-${separator}  one${separator}  two${separator}").Object
            assertEquals(1, root["a"].Int)
            assertEquals("one\ntwo", root["b"].String)
        }
    }

    @Test
    fun scalarKeysAreAccessibleByName() {
        val root = Yaml.parse("1: one\ntrue: boolean\nnull: empty\n\"a:b\": colon\n'quoted key': value").Object
        assertEquals("one", root["1"].String)
        assertEquals("boolean", root["true"].String)
        assertEquals("empty", root["null"].String)
        assertEquals("colon", root["a:b"].String)
    }

    @Test
    fun duplicateKeyPolicyAndLiteralMergeKey() {
        assertFailsWith<YamlParseException> { Yaml.parse("a: 1\na: 2") }
        assertFailsWith<YamlParseException> { Yaml.parse("{a: 1, a: 2}") }
        val permissive = Yaml(YamlConfiguration(allowDuplicateKeys = true))
        assertEquals(2, permissive.parse("a: 1\na: 2").Object["a"].Int)
        assertEquals(1, Yaml.parse("\"<<\": 1").Object["<<"].Int)
        val literal = Yaml(YamlConfiguration(mergeKeys = false))
        assertEquals(1, literal.parse("<<: 1").Object["<<"].Int)
    }

    @Test
    fun malformedInputHasOriginalSourceLocations() {
        val error = assertFailsWith<YamlParseException> { Yaml.parse("a: 1\r\na: 2") }
        assertEquals(2, error.line)
        assertEquals(1, error.column)
        assertEquals(6, error.offset)
        assertTrue(error.message!!.contains("Duplicate"))
        for (text in ["a:\n\tb: 1", "[1, 2", "{a: 1", "\"unterminated", "a: value\n  b: 2", "a: [1,,2]", "@reserved"]) {
            assertFailsWith<YamlParseException>(text) { Yaml.parse(text) }
        }
    }

    @Test
    fun unsupportedGraphsAndComplexKeysFailExplicitly() {
        for (text in ["a: *missing", "a: &a [*a]", "? [a, b]\n: value", "[a, b]: value", "a: &n 1\nb: &m *n", "<<: [1]"]) {
            assertFailsWith<YamlParseException>(text) { Yaml.parse(text) }
        }
    }

    @Test
    fun plainKeysCanContainBracketAndQuoteCharacters() {
        val value = Yaml.parse("- bla\"keks: foo\n- bla]keks: foo\n- bla[keks: foo").Array
        assertEquals("foo", value[0].Object["bla\"keks"].String)
        assertEquals("foo", value[1].Object["bla]keks"].String)
        assertEquals("foo", value[2].Object["bla[keks"].String)
        assertEquals("bar", Yaml.parse("{?foo: bar}").Object["?foo"].String)
    }

    @Test
    fun plainContinuationAllowsIndicatorCharacters() {
        assertEquals("hello !world &data", Yaml.parse("value: hello\n  !world\n  &data").Object["value"].String)
        assertEquals("single multiline - sequence entry", Yaml.parse("- single multiline\n - sequence entry").Array[0].String)
        assertEquals("x x", Yaml.parse("x:\n - x\n  \tx").Object.arr("x")[0].String)
        assertEquals("value", Yaml.parse("{multi\n line: value}").Object["multi line"].String)
    }

    @Test
    fun escapedTrailingWhitespaceSurvivesQuoteFolding() {
        assertEquals("trailing\t tab", Yaml.parse("\"trailing\\t  \n    tab\"").String)
        assertEquals("trailing  next", Yaml.parse("\"trailing\\ \n    next\"").String)
        assertEquals("one\ntwo", Yaml.parse("\"one\\\n\n  two\"").String)
    }

    @Test
    fun foldedTabsAndCommentsAfterBlockScalars() {
        assertEquals("foo \n\n\t bar\n\nbaz\n", Yaml.parse(">\n  foo \n \n  \t bar\n\n  baz\n").String)
        assertEquals("literal\n", Yaml.parse("|\n  literal\n # outside comment\n").String)
        assertEquals("\t\ndetected\n", Yaml.parse(">\n \t\n detected\n").String)
    }

    @Test
    fun propertiesOnScalarKeys() {
        val root = Yaml.parse("&a a: &b b\n*b : *a\n!!str 23: !!bool false").Object
        assertEquals("b", root["a"].String)
        assertEquals("a", root["b"].String)
        assertFalse(root["23"].Boolean)
        assertSame(YamlNull, Yaml.parse("# empty document\n...\n"))
    }

    @Test
    fun malformedFlowAndCompactIndentation() {
        for (text in ["flow: [a,\nb,\nc]", "quoted: \"a\nb\"", "[a,#comment\n]", "[key\n  : value]",
            "key: [word1\n# comment\n  word2]", "[-]", "[-, -]", "&anchor - item", "-\t-\n", "--- key: value\n    next: value"]) {
            assertFailsWith<YamlParseException>(text) { Yaml.parse(text) }
        }
    }
}
