package androidx.compose.ui.text.intl

import androidx.compose.runtime.Immutable
import androidx.compose.ui.InternalComposeUiApi
import org.jetbrains.skia.icu.LocaleUtils

@Immutable
actual class Locale actual constructor(languageTag: String) {
    private val identity = LocaleUtils.localeInfo(languageTag)

    actual val language: String get() = identity.language
    actual val script: String get() = identity.script
    actual val region: String get() = identity.region

    actual fun toLanguageTag(): String = identity.languageTag

    actual override fun equals(other: Any?): Boolean = other is Locale && toLanguageTag() == other.toLanguageTag()

    actual override fun hashCode(): Int = toLanguageTag().hashCode()

    actual override fun toString(): String = toLanguageTag()

    actual companion object {
        @Suppress("DEPRECATION")
        actual val current: Locale get() = platformLocaleDelegate.current[0]
    }

    internal val isRightToLeft: Boolean get() = identity.isRightToLeft
}

@InternalComposeUiApi
actual fun Locale.isRtl(): Boolean = isRightToLeft
