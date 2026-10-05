package love.yinlin.foundation.parser.toml

import kotlin.test.*

class TestTomlScalars {
    private fun read(value: String): TomlPrimitive = Toml.parse("value = $value").Object["value"] as TomlPrimitive

    @Test
    fun integerRadicesAndUnderscores() {
        for ([source, expected] in mapOf("+99" to 99L, "-17" to -17L, "-0" to 0L, "+0" to 0L,
            "1_000" to 1000L, "0xDEAD_beef" to 3735928559L, "0o755" to 493L, "0b1101_0110" to 214L)) {
            val value = read(source)
            assertEquals(TomlScalarKind.Integer, value.kind)
            assertEquals(source, value.String)
            assertEquals(expected, value.Long)
        }
    }

    @Test
    fun signed64BitBoundaries() {
        assertEquals(Long.MAX_VALUE, read("9223372036854775807").Long)
        assertEquals(Long.MIN_VALUE, read("-9223372036854775808").Long)
        assertEquals(Long.MAX_VALUE, read("0x7fff_ffff_ffff_ffff").Long)
        assertEquals(Long.MAX_VALUE, read("0o777777777777777777777").Long)
        assertEquals(Long.MAX_VALUE, read("0b" + "1".repeat(63)).Long)
        for (text in ["9223372036854775808", "-9223372036854775809", "0x8000000000000000", "0o1000000000000000000000", "0b1" + "0".repeat(63)])
            assertFailsWith<TomlParseException> { read(text) }
    }

    @Test
    fun invalidNumberGrammar() {
        for (text in ["00", "01", "+01", "-01", "0_0", "1_", "_1", "1__0", "0x_1",
            "+0x1", "-0b1", "0X1", "0x", "0o8", "0b2", ".5", "1.", "1.e2", "01.2", "1e",
            "1e+", "1e_1", "1e1_", "1_.0", "1._0", "1.0_e2", "1.0e1.2", "NaN", "Infinity"])
            assertFailsWith<TomlParseException>(text) { read(text) }
    }

    @Test
    fun decimalFloatsRetainPrecision() {
        for ([source, expected] in mapOf("+1.0" to 1.0, "-0.01" to -0.01, "5e+22" to 5e22,
            "1e06" to 1e6, "-2E-2" to -0.02, "6.626e-34" to 6.626e-34, "1_000.5_0" to 1000.5)) {
            assertEquals(TomlScalarKind.Float, read(source).kind)
            assertEquals(expected, read(source).Double)
        }
        val text = "1.12345678901234567890123456789"
        assertEquals(text, read(text).String)
        assertEquals(text, read(text).toString())
    }

    @Test
    fun specialFloatsAndNegativeZero() {
        for (text in ["nan", "+nan", "-nan"]) assertTrue(read(text).Double.isNaN())
        for (text in ["inf", "+inf"]) assertEquals(Double.POSITIVE_INFINITY, read(text).Double)
        assertEquals(Double.NEGATIVE_INFINITY, read("-inf").Double)
        assertEquals((-0.0).toBits(), read("-0.0").Double.toBits())
        assertEquals("inf", TomlPrimitive(Double.POSITIVE_INFINITY).String)
        assertEquals("-inf", TomlPrimitive(Float.NEGATIVE_INFINITY).String)
        assertEquals("nan", TomlPrimitive(Double.NaN).String)
    }

    @Test
    fun booleanAndTypedAccessors() {
        assertTrue(read("true").Boolean)
        assertEquals(false, read("false").Boolean)
        assertNull(read("1").BooleanNull)
        assertNull(read("1.0").LongNull)
        assertEquals(Int.MIN_VALUE, read("-2147483648").Int)
        assertFailsWith<NullPointerException> { read("true").Int }
        assertEquals(123, TomlPrimitive("123").Int)
        assertEquals("0x10", TomlPrimitive.number("0x10").String)
        assertFailsWith<IllegalArgumentException> { TomlPrimitive.number("0_1") }
    }

    @Test
    fun allFourDateTimeTypes() {
        for ([text, type] in mapOf(
            "1979-05-27T07:32:00Z" to TomlScalarKind.OffsetDateTime,
            "1979-05-27t07:32:00z" to TomlScalarKind.OffsetDateTime,
            "1979-05-27 07:32:00-07:00" to TomlScalarKind.OffsetDateTime,
            "1979-05-27T07:32:00.999999999999+01:30" to TomlScalarKind.OffsetDateTime,
            "1979-05-27T07:32:00" to TomlScalarKind.LocalDateTime,
            "2000-02-29" to TomlScalarKind.LocalDate,
            "07:32:00.999999999999" to TomlScalarKind.LocalTime,
        )) {
            val value = read(text)
            assertEquals(type, value.kind)
            assertEquals(text, value.String)
            assertTrue(value.isDateTime)
            assertEquals(value, TomlPrimitive.dateTime(text))
            assertNull(value.DoubleNull)
        }
    }

    @Test
    fun optionalSecondsRequireVersion11() {
        val strict = Toml(TomlConfiguration(version = TomlVersion.V1_0))
        for (text in ["07:32", "1979-05-27T07:32", "1979-05-27 07:32Z", "1979-05-27T07:32-07:00"]) {
            assertTrue(read(text).isDateTime)
            assertFailsWith<TomlParseException>(text) { strict.parse("value = $text") }
            assertFailsWith<IllegalArgumentException> { strict.encodeToString(toml { "value" with TomlPrimitive.dateTime(text) }) }
        }
    }

    @Test
    fun invalidDatesAndTimes() {
        for (text in ["1900-02-29", "2023-02-29", "2024-04-31", "2024-00-01", "2024-13-01",
            "2024-01-00", "2024-01-32", "24:00:00", "07:60:00", "07:32:60", "07:32:61", "7:32:00", "07:32:00.",
            "2024-01-01T07:32:00+24:00", "2024-01-01T07:32:00+01:60", "07:32:00Z",
            "2024-01-01  07:32:00", "2024-01-01\t07:32:00", "2024-01-01T07:32:00+0100", "07:32.1"])
            assertFailsWith<TomlParseException>(text) { read(text) }
        assertFailsWith<IllegalArgumentException> { TomlPrimitive.dateTime("not a date") }
    }
}