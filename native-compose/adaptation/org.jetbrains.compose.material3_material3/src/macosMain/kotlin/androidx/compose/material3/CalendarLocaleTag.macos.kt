package androidx.compose.material3

import org.jetbrains.skia.icu.LocaleUtils
import platform.Foundation.localeIdentifier

internal actual fun calendarLocaleLanguageTag(locale: CalendarLocale): String = LocaleUtils.localeInfo(locale.localeIdentifier).languageTag
