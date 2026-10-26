package androidx.compose.material3.internal

import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.calendarLocaleLanguageTag
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.jetbrains.skia.icu.DateTimeFormat
import org.jetbrains.skia.icu.DateTimeUtils

/** Uses one UTC/Gregorian implementation on every owned desktop target, retaining locale-sensitive parsing. */
internal actual class PlatformDateFormat actual constructor(locale: CalendarLocale) {
    private val languageTag = calendarLocaleLanguageTag(locale)
    private val information by lazy { DateTimeUtils.localeInfo(languageTag) }

    actual val firstDayOfWeek: Int get() = information.firstDayOfWeek
    actual val weekdayNames: List<Pair<String, String>> get() = information.weekdayNames

    actual fun formatWithPattern(utcTimeMillis: Long, pattern: String, cache: MutableMap<String, Any>): String = formatter(pattern, languageTag, cache).format(utcTimeMillis)

    actual fun formatWithSkeleton(utcTimeMillis: Long, skeleton: String, cache: MutableMap<String, Any>): String {
        val pattern = cache.getOrPut("rachel:date-skeleton:$languageTag:$skeleton") { DateTimeUtils.bestPattern(skeleton, languageTag) } as String
        return formatter(pattern, languageTag, cache).format(utcTimeMillis)
    }

    actual fun parse(date: String, pattern: String, locale: CalendarLocale, cache: MutableMap<String, Any>): CalendarDate? {
        val milliseconds = formatter(pattern, calendarLocaleLanguageTag(locale), cache).parse(date) ?: return null
        return Instant.fromEpochMilliseconds(milliseconds).toCalendarDate(TimeZone.UTC)
    }

    actual fun getDateInputFormat(): DateInputFormat = datePatternAsInputFormat(information.shortDatePattern)
    actual fun is24HourFormat(): Boolean = information.is24HourFormat

    private fun formatter(pattern: String, languageTag: String, cache: MutableMap<String, Any>): DateTimeFormat = cache.getOrPut("rachel:date-format:$languageTag:$pattern") {
        DateTimeFormat.make(pattern, languageTag)
    } as DateTimeFormat
}
