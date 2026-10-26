package androidx.compose.material3

import androidx.compose.material3.internal.PlatformDateFormat
import androidx.compose.ui.text.intl.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Material3CalendarTest {
    @Test
    fun formattedDatesParseBackWithoutAcceptingInvalidCalendarDates() {
        val locale = Locale("en-GB")
        val format = PlatformDateFormat(locale)
        val cache = mutableMapOf<String, Any>()
        assertEquals("1970-01-01", format.formatWithPattern(0, "yyyy-MM-dd", cache))
        assertEquals(0L, format.parse("1970-01-01", "yyyy-MM-dd", locale, cache)?.utcTimeMillis)
        assertNull(format.parse("2023-02-29", "yyyy-MM-dd", locale, cache))
        assertEquals(1, format.firstDayOfWeek)
        assertEquals(7, format.weekdayNames.size)
    }

    @Test
    fun localizedIntegerFormattingHonorsPaddingAndMaximumDigits() {
        val locale = Locale("ar-EG")
        assertEquals("٢٠٢٦", 2026.toLocalString(locale = locale))
        assertEquals("٠٣", 3.toLocalString(minDigits = 2, locale = locale))
        assertEquals("٢٦", 2026.toLocalString(maxDigits = 2, locale = locale))
    }
}
