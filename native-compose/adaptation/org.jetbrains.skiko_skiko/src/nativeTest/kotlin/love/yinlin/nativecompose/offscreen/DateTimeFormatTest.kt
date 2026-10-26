package love.yinlin.nativecompose.offscreen

import org.jetbrains.skia.icu.DateTimeFormat
import org.jetbrains.skia.icu.DateTimeUtils
import org.jetbrains.skia.impl.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DateTimeFormatTest {
    @Test
    fun datesUseUtcStrictParsingAndProlepticGregorianYears() {
        DateTimeFormat.make("yyyy-MM-dd", "en-US").use { format ->
            assertEquals("1970-01-01", format.format(0))
            assertEquals(0L, format.parse("1970-01-01"))
            assertNull(format.parse("2023-02-29"))
            assertNull(format.parse("2024-02-29suffix"))
            assertEquals("1500-03-01", format.format(-14826672000000L))
        }
    }

    @Test
    fun localeConventionsAndDigitsAreRespected() {
        assertEquals(7, DateTimeUtils.localeInfo("en-US").firstDayOfWeek)
        assertEquals(1, DateTimeUtils.localeInfo("en-GB").firstDayOfWeek)
        assertEquals("Monday", DateTimeUtils.localeInfo("en-US").weekdayNames.first().first)
        assertEquals("٢٠٢٦", DateTimeUtils.formatNumber(2026, 1, 40, false, "ar-EG"))
        assertEquals("12,34,567", DateTimeUtils.formatNumber(1234567, 1, 40, true, "en-IN"))
    }

    @Test
    fun closedFormattersAndUnboundedInputsAreRejected() {
        val format = DateTimeFormat.make("yyyy-MM-dd", "en-GB")
        format.close()
        assertFailsWith<IllegalStateException> { format.format(0) }
        assertFailsWith<IllegalArgumentException> { DateTimeFormat.make("yyyy", "en\u0000US") }
        assertFailsWith<IllegalArgumentException> { DateTimeUtils.formatNumber(1, 0, 101, false, "en-US") }
    }
}
