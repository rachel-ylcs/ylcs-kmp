package love.yinlin.foundation.parser.arguments

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestArgumentValues {
    @Test
    fun firstValueAndListAccess() {
        val single = Argument("first")
        assertEquals("first", single.first)
        assertEquals("first", single.firstOrNull)
        assertEquals("first", single[0])
        assertEquals(1, single.size)
        assertFalse(single is Argument.Values)
        val multiple: Argument = ["first", "second"]
        assertTrue(multiple is Argument.Values)
        assertEquals("first", multiple.first)
        assertEquals("second", multiple[1])
        assertEquals(["first", "second"], multiple.toList())
        assertTrue("second" in multiple)
        assertEquals(Argument(["first", "second"]), multiple)
        assertEquals(Argument("first").hashCode(), single.hashCode())
    }

    @Test
    fun emptySingletonAndInputSnapshot() {
        assertSame(Argument.Empty, Argument([]))
        assertNull(Argument.Empty.firstOrNull)
        assertEquals(0, Argument.Empty.size)
        assertEquals("", Argument("").first)
        val values: MutableList<String> = ["first", "second"]
        val argument = Argument(values)
        values[0] = "changed"
        values.add("third")
        assertEquals(["first", "second"], (argument as Argument.Values).values)
        val input: Array<String> = ["a=1"]
        val args = Arguments(input)
        input[0] = "a=2"
        assertEquals(1, args["a"].Int)
    }

    @Test
    fun booleanConversionIsStrict() {
        assertTrue(Argument("true").Boolean)
        assertFalse(Argument("false").Boolean)
        assertEquals(true, Argument("true").BooleanNull)
        assertEquals(false, Argument("false").BooleanNull)
        for (text in ["TRUE", "False", "1", "0", "yes", " true ", ""]) {
            assertNull(Argument(text).BooleanNull, text)
            assertFailsWith<ArgumentStateException>(text) { Argument(text).Boolean }
        }
    }

    @Test
    fun signedIntegersAndBoundaries() {
        assertEquals(Byte.MIN_VALUE, Argument("-128").Byte)
        assertEquals(Byte.MAX_VALUE, Argument("127").ByteNull)
        assertEquals(Short.MIN_VALUE, Argument("-32768").ShortNull)
        assertEquals(Short.MAX_VALUE, Argument("32767").Short)
        assertEquals(Int.MIN_VALUE, Argument("-2147483648").Int)
        assertEquals(Int.MAX_VALUE, Argument("2147483647").IntNull)
        assertEquals(Long.MIN_VALUE, Argument("-9223372036854775808").Long)
        assertEquals(Long.MAX_VALUE, Argument("9223372036854775807").LongNull)
        assertEquals(42, Argument("+42").Int)
        assertEquals(42L, Argument("0042").Long)
        assertNull(Argument("128").ByteNull)
        assertNull(Argument("32768").ShortNull)
        assertNull(Argument("2147483648").IntNull)
        assertNull(Argument("9223372036854775808").LongNull)
        assertFailsWith<ArgumentStateException> { Argument("-129").Byte }
        assertFailsWith<ArgumentStateException> { Argument("-32769").Short }
        assertFailsWith<ArgumentStateException> { Argument("-2147483649").Int }
        assertFailsWith<ArgumentStateException> { Argument("-9223372036854775809").Long }
        for (text in ["1.0", "0x10", "1_000", " 1 ", "value", ""]) {
            assertNull(Argument(text).IntNull, text)
            assertFailsWith<ArgumentStateException>(text) { Argument(text).Int }
        }
    }

    @Test
    fun unsignedIntegersAndBoundaries() {
        assertEquals(UByte.MAX_VALUE, Argument("255").UByte)
        assertEquals(UByte.MIN_VALUE, Argument("0").UByteNull)
        assertEquals(UShort.MAX_VALUE, Argument("65535").UShort)
        assertEquals(UShort.MIN_VALUE, Argument("0").UShortNull)
        assertEquals(UInt.MAX_VALUE, Argument("4294967295").UInt)
        assertEquals(UInt.MIN_VALUE, Argument("0").UIntNull)
        assertEquals(ULong.MAX_VALUE, Argument("18446744073709551615").ULong)
        assertEquals(ULong.MIN_VALUE, Argument("0").ULongNull)
        assertNull(Argument("256").UByteNull)
        assertNull(Argument("65536").UShortNull)
        assertNull(Argument("4294967296").UIntNull)
        assertNull(Argument("18446744073709551616").ULongNull)
        assertFailsWith<ArgumentStateException> { Argument("-1").UByte }
        assertFailsWith<ArgumentStateException> { Argument("-1").UShort }
        assertFailsWith<ArgumentStateException> { Argument("-1").UInt }
        assertFailsWith<ArgumentStateException> { Argument("-1").ULong }
    }

    @Test
    fun floatingPointAndCharacterConversion() {
        assertEquals(1.25f, Argument("1.25").Float)
        assertEquals(-0.5f, Argument("-5e-1").FloatNull)
        assertEquals(12.5, Argument("1.25e1").Double)
        assertEquals(-0.5, Argument("-0.5").DoubleNull)
        assertTrue(Argument("NaN").Double.isNaN())
        assertEquals(Double.POSITIVE_INFINITY, Argument("Infinity").Double)
        assertNull(Argument("abc").DoubleNull)
        assertNull(Argument("abc").FloatNull)
        assertFailsWith<ArgumentStateException> { Argument("abc").Double }
        assertFailsWith<ArgumentStateException> { Argument("abc").Float }
        assertEquals('中', Argument("中").Char)
        assertEquals('a', Argument("a").CharNull)
        assertNull(Argument("ab").CharNull)
        assertNull(Argument("").CharNull)
        assertFailsWith<ArgumentStateException> { Argument("ab").Char }
    }

    @Test
    fun conversionsAlwaysUseTheFirstValue() {
        val args = Arguments("number=42 number=123 bool=false bool=true text= text=second")
        assertEquals(42, args["number"].Int)
        assertEquals(42.0, args["number"].Double)
        assertFalse(args["bool"].Boolean)
        assertEquals("", args["text"].String)
        val badFirst = Arguments("x=bad x=123")["x"]
        assertNull(badFirst.IntNull)
        assertFailsWith<ArgumentStateException> { badFirst.Int }
    }

    @Test
    fun missingAndValuelessTypedAccess() {
        val strict: List<(Argument?) -> Any> = [
            { it.String }, { it.Boolean }, { it.Byte }, { it.Short }, { it.Int }, { it.Long },
            { it.UByte }, { it.UShort }, { it.UInt }, { it.ULong }, { it.Float }, { it.Double }, { it.Char },
        ]
        val nullable: List<(Argument?) -> Any?> = [
            { it.StringNull }, { it.BooleanNull }, { it.ByteNull }, { it.ShortNull }, { it.IntNull }, { it.LongNull },
            { it.UByteNull }, { it.UShortNull }, { it.UIntNull }, { it.ULongNull }, { it.FloatNull }, { it.DoubleNull }, { it.CharNull },
        ]
        for (argument in [null, Argument.Empty]) {
            for (access in strict) assertFailsWith<ArgumentStateException> { access(argument) }
            for (access in nullable) assertNull(access(argument))
        }
    }

    @Test
    fun invalidValueIndexesThrowStateErrors() {
        assertFailsWith<ArgumentStateException> { Argument.Empty.first }
        assertFailsWith<ArgumentStateException> { Argument.Empty[0] }
        assertFailsWith<ArgumentStateException> { Argument("a")[-1] }
        assertFailsWith<ArgumentStateException> { Argument("a")[1] }
        assertFailsWith<ArgumentStateException> { Argument("a")[Int.MAX_VALUE] }
    }
}
