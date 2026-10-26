package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.intl.Locale

actual typealias CalendarLocale = Locale

@Composable
@ReadOnlyComposable
internal actual fun defaultLocale(): CalendarLocale = LocalLocale.current

internal actual fun calendarLocaleLanguageTag(locale: CalendarLocale): String = locale.toLanguageTag()
