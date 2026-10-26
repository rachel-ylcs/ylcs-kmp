@file:Suppress("DEPRECATION")

package org.jetbrains.compose.resources

import androidx.compose.ui.text.intl.Locale
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.window.NativeDesktop

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locale = NativeDesktop.preferredLocales().firstOrNull()?.let(::Locale) ?: Locale.current
    return ResourceEnvironment(
        language = LanguageQualifier(locale.language),
        script = ScriptQualifier(locale.script),
        region = RegionQualifier(locale.region),
        theme = ThemeQualifier.selectByValue(NativeDesktop.systemTheme() == SystemTheme.DARK),
        density = DensityQualifier.selectByDensity(resourceDisplayScale()),
    )
}
