package androidx.compose.material3

import androidx.compose.ui.text.intl.Locale
import org.jetbrains.skia.icu.DateTimeUtils

/** Honor localized digits and grouping instead of the upstream non-JVM zero-padding fallback. */
@Suppress("DEPRECATION")
internal actual fun Int.toLocalString(minDigits: Int, maxDigits: Int, isGroupingUsed: Boolean, locale: CalendarLocale?): String {
    val maximum = maxDigits.coerceIn(0, 100)
    val minimum = minDigits.coerceIn(0, maximum)
    val languageTag = if (locale == null) Locale.current.toLanguageTag() else calendarLocaleLanguageTag(locale)
    return DateTimeUtils.formatNumber(toLong(), minimum, maximum, isGroupingUsed, languageTag)
}
