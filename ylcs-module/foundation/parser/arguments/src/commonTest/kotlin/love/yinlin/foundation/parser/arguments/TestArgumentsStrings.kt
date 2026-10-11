package love.yinlin.foundation.parser.arguments

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class TestArgumentsStrings {
    @Test
    fun equalsInsideQuotedValues() {
        val args = Arguments("a=\"x=y\" b=\"=\" c=\"==a===b==\"")
        assertEquals("x=y", args["a"].String)
        assertEquals("=", args["b"].String)
        assertEquals("==a===b==", args["c"].String)
        assertEquals(args, Arguments(["a=\"x=y\"", "b=\"=\"", "c=\"==a===b==\""]))
    }

    @Test
    fun backslashesAreLiteralOutsideQuotes() {
        val args = Arguments("""v=\\456 path=C:\Desktop slash=\ \flag value=\n single='text'""")
        assertEquals("\\\\456", args["v"].String)
        assertEquals("C:\\Desktop", args["path"].String)
        assertEquals("\\", args["slash"].String)
        assertEquals("\\n", args["value"].String)
        assertEquals("'text'", args["single"].String)
        assertEquals(Argument.Empty, args["\\flag"])
    }

    @Test
    fun onlyDoubleQuotesAreEscapedInsideQuotes() {
        val args = Arguments("""path="C:\Desktop" literal="\n\r\t\u0041\a\\end" quote="\"haha\"" """)
        assertEquals("C:\\Desktop", args["path"].String)
        assertEquals("\\n\\r\\t\\u0041\\a\\\\end", args["literal"].String)
        assertEquals("\"haha\"", args["quote"].String)
    }

    @Test
    fun everyBackslashQuotePairEscapesJustTheQuote() {
        // 不采用 shell 的奇偶反斜杠规则，也不把两个反斜杠合并为一个。
        for (count in 1..8) {
            val slashes = "\\".repeat(count)
            val token = "value=\"$slashes\"end\""
            val expected = "\\".repeat(count - 1) + "\"end"
            assertEquals(expected, Arguments(token)["value"].String)
            assertEquals(expected, Arguments([token])["value"].String)
        }
    }

    @Test
    fun unicodeAndQuotedPunctuation() {
        val args = Arguments("中文=\"你好 🌍 = 值\" symbols=\"' # -- ; | & ( )\"")
        assertEquals("你好 🌍 = 值", args["中文"].String)
        assertEquals("' # -- ; | & ( )", args["symbols"].String)
    }

    @Test
    fun generatedQuotedValuesPreserveContent() {
        val random = Random(42)
        val alphabet = "abc =\\\"\t\n中文"
        val tokens = ArrayList<String>()
        val expected = linkedMapOf<String, MutableList<String>>()
        repeat(600) {
            val key = "key${random.nextInt(20)}"
            // 结尾不是反斜杠，避免把字符串的闭合引号转义。
            val value = buildString {
                repeat(random.nextInt(80)) { append(alphabet[random.nextInt(alphabet.length)]) }
                append('x')
            }
            tokens.add("$key=\"${value.replace("\"", "\\\"")}\"")
            expected.getOrPut(key) { ArrayList() }.add(value)
        }
        val commandLine = Arguments(tokens.joinToString(" \t"))
        val array = Arguments(tokens.toTypedArray())
        assertEquals(commandLine, array)
        for ([key, values] in expected) assertEquals(values, (commandLine[key] as Argument.Values).values)
    }

    @Test
    fun largeInputsAndManyRepeatedValues() {
        val count = 20000
        val args = Arguments(Array(count) { "value=$it" })
        assertEquals(count, args["value"]!!.size)
        assertEquals("0", args["value"]!!.first)
        assertEquals((count - 1).toString(), args["value"]!![count - 1])
        assertEquals(args, Arguments((0 ..< count).joinToString(" ") { "value=$it" }))
        val value = "a=b ".repeat(25000)
        assertEquals(value, Arguments("value=\"$value\"")["value"].String)
        val escaped = "\\\"".repeat(10000)
        assertEquals("\"".repeat(10000), Arguments("value=\"$escaped\"")["value"].String)
    }
}
