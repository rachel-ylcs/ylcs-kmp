package love.yinlin.foundation.parser.arguments

import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestArguments {
    @Test
    fun exampleCommandLine() {
        val args = Arguments("""test.exe a=123 b= --version=4.5.0 enabled c="x=y" v=\\456 l=true path="C:\Desktop" path="\"haha\"" """)
        assertEquals("test.exe", args.executable)
        assertEquals(["a", "b", "--version", "enabled", "c", "v", "l", "path"], args.keys.toList())
        assertEquals("123", args["a"].String)
        assertEquals("", args["b"].String)
        assertEquals("4.5.0", args["--version"].String)
        assertSame(Argument.Empty, args["enabled"])
        assertEquals("x=y", args["c"].String)
        assertEquals("\\\\456", args["v"].String)
        assertTrue(args["l"].Boolean)
        assertEquals(["C:\\Desktop", "\"haha\""], (args["path"] as Argument.Values).values)
        assertTrue(args["path"] is Argument.Values)
    }

    @Test
    fun exampleArray() {
        val args = Arguments(["test.exe", "a=123", "b=", "--version=4.5.0", "enabled", "c=\"x=y\"", "v=\\\\456", "l=true", "path=\"C:\\Desktop\"", "path=\"\\\"haha\\\"\""])
        assertEquals("test.exe", args.executable)
        assertEquals(123, args["a"].Int)
        assertEquals("", args["b"].String)
        assertSame(Argument.Empty, args["enabled"])
        assertEquals("x=y", args["c"].String)
        assertEquals("\\\\456", args["v"].String)
        assertEquals(listOf("C:\\Desktop", "\"haha\""), (args["path"] as Argument.Values).values)
    }

    @Test
    fun emptyInputs() {
        for (args in listOf(Arguments(""), Arguments(" \t\r\n "), Arguments([]))) {
            assertNull(args.executable)
            assertTrue(args.isEmpty)
            assertNull(args["missing"])
        }
    }

    @Test
    fun parametersOnly() {
        for (input in ["a=123 enabled", "enabled a=123", "--version=1 a=123", "-v a=123", "file.exe=data", "path=C:\\Desktop", "file=/usr/bin/test"]) {
            val args = Arguments(input)
            assertNull(args.executable, input)
            assertTrue(args.isNotEmpty)
            assertEquals(args, Arguments(input.split(' ').toTypedArray()), input)
        }
        assertSame(Argument.Empty, Arguments("enabled")["enabled"])
        assertSame(Argument.Empty, Arguments(["tool"])["tool"])
    }

    @Test
    fun executableDetection() {
        for (path in ["test.exe", "TEST.EXE", "tool.com", "tool.cmd", "tool.bat", "tool.sh", "tool.bin", "tool.kexe", "tool.app", "tool.out", "tool.jar", "/usr/bin/test", "./tool", "../tool", "C:\\Apps\\tool", "\\\\server\\share\\tool"]) {
            val args = Arguments("$path enabled a=1")
            assertEquals(path, args.executable, path)
            assertEquals(args, Arguments([path, "enabled", "a=1"]))
            assertEquals(path, Arguments(path).executable)
            assertTrue(Arguments([path]).isEmpty)
        }
    }

    @Test
    fun quotedExecutableAndSpacesInArrayPath() {
        val path = "C:\\Program Files\\tool.exe"
        val args = Arguments("\"$path\" a=1")
        assertEquals(path, args.executable)
        assertEquals(args, Arguments([path, "a=1"]))
        assertEquals(args, Arguments(["\"$path\"", "a=1"]))
        assertEquals("tool", Arguments("\"tool\" enabled").executable)
        assertEquals("/apps/a=b/tool", Arguments("\"/apps/a=b/tool\" a=1").executable)
        assertEquals("/apps/\"tool\"", Arguments("\"/apps/\\\"tool\\\"\" a=1").executable)
    }

    @Test
    fun onlyFirstItemCanBeExecutable() {
        val args = Arguments("enabled test.exe /usr/bin/test ./tool C:\\tool.exe")
        assertNull(args.executable)
        for (key in args.keys) assertSame(Argument.Empty, args[key])
        assertEquals(5, args.size)
        assertSame(Argument.Empty, Arguments("tool.exe another.exe")["another.exe"])
    }

    @Test
    fun optionNamesAreKeptVerbatim() {
        val args = Arguments("-v --version=1 -- --path/tool --tool.exe KEY=1 key=2 中文=值 a.b=3")
        assertNull(args.executable)
        for (key in ["-v", "--", "--path/tool", "--tool.exe"]) assertSame(Argument.Empty, args[key])
        assertEquals("1", args["--version"].String)
        assertEquals("1", args["KEY"].String)
        assertEquals("2", args["key"].String)
        assertEquals("值", args["中文"].String)
        assertEquals("3", args["a.b"].String)
    }

    @Test
    fun whitespaceSeparatesTokensOnlyOutsideQuotes() {
        val args = Arguments(" \ta=1\r\nb=\tflag  c=\" hello\tworld\n \" \u2003d=4 ")
        assertEquals("1", args["a"].String)
        assertEquals("", args["b"].String)
        assertSame(Argument.Empty, args["flag"])
        assertEquals(" hello\tworld\n ", args["c"].String)
        assertEquals("4", args["d"].String)
        assertEquals(5, args.size)
    }

    @Test
    fun whitespaceAfterEqualsStartsTheNextParameter() {
        val args = Arguments("a= b c= d=2")
        assertEquals("", args["a"].String)
        assertSame(Argument.Empty, args["b"])
        assertEquals("", args["c"].String)
        assertEquals("2", args["d"].String)
    }

    @Test
    fun emptyValuesAndFlagsAreDifferent() {
        val args = Arguments("a= b=\"\" enabled other enabled")
        assertEquals([""], (args["a"] as Argument.Values).values)
        assertEquals([""], (args["b"] as Argument.Values).values)
        assertFalse(args["a"] is Argument.Empty)
        assertSame(Argument.Empty, args["enabled"])
        assertSame(args["enabled"], args["other"])
        assertTrue(args["enabled"] is Argument.Empty)
        assertFalse(args["enabled"] is Argument.Values)
    }

    @Test
    fun repeatedKeysKeepAllValuesAndInsertionOrder() {
        val args = Arguments("x=1 y=2 x=3 x= x=\"\" y=4")
        assertEquals(["x", "y"], args.keys.toList())
        assertEquals(["1", "3", "", ""], (args["x"] as Argument.Values).values)
        assertEquals(["2", "4"], (args["y"] as Argument.Values).values)
        assertEquals("1", args["x"]!!.first)
        assertEquals("3", args["x"]!![1])
        assertTrue(args["x"] is Argument.Values)
    }

    @Test
    fun flagsDoNotEraseAssignments() {
        for (input in ["x x=1 x x=2", "x=1 x=2 x"]) {
            assertEquals(["1", "2"], (Arguments(input)["x"] as Argument.Values).values)
            assertEquals(Arguments(input), Arguments(input.split(' ').toTypedArray()))
        }
    }

    @Test
    fun mapAccessAndValueEquality() {
        val args = Arguments("tool.exe a=1 flag a=2")
        assertTrue("a" in args)
        assertFalse("missing" in args)
        assertEquals(2, args.size)
        assertEquals(args, Arguments(["tool.exe", "a=1", "flag", "a=2"]))
        assertEquals(args.hashCode(), Arguments("tool.exe a=1 flag a=2").hashCode())
        assertNotEquals(args, Arguments("other.exe a=1 flag a=2"))
        assertNotEquals(args, Arguments("tool.exe a=2 flag a=1"))
    }

    @Test
    fun serializeArguments() {
        val args = Arguments("tool.exe a=1 flag a=2")
        val jsonString = args.toJsonString()
        assertEquals(args, jsonString.parseJsonValue<Arguments>())
    }
}
