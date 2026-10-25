package androidx.compose.ui.text.platform

import androidx.compose.ui.text.PlatformStringDelegate
import androidx.compose.ui.text.intl.Locale
import org.jetbrains.skia.icu.CharProperties
import org.jetbrains.skia.icu.LocaleUtils

/** ICU preserves contextual casing, multi-character mappings and UTF-16 offsets. */
internal class IcuStringDelegate : PlatformStringDelegate {
    override fun toUpperCase(string: String, locale: Locale): String = LocaleUtils.uppercase(string, locale.toLanguageTag())

    override fun toLowerCase(string: String, locale: Locale): String = LocaleUtils.lowercase(string, locale.toLanguageTag())

    override fun capitalize(string: String, locale: Locale): String {
        if (string.isEmpty()) return string
        val length = initialCodePointLength(string)
        val codePoint = if (length == 2) ((string[0].code - 0xD800) shl 10) + string[1].code - 0xDC00 + 0x10000 else string[0].code
        if (!CharProperties.codePointHasBinaryProperty(codePoint, CharProperties.LOWERCASE)) return string
        return LocaleUtils.titlecase(string.substring(0, length), locale.toLanguageTag()) + string.substring(length)
    }

    override fun decapitalize(string: String, locale: Locale): String {
        if (string.isEmpty()) return string
        val length = initialCodePointLength(string)
        return LocaleUtils.lowercase(string.substring(0, length), locale.toLanguageTag()) + string.substring(length)
    }

    private fun initialCodePointLength(string: String): Int = if (string.length > 1 && string[0].isHighSurrogate() && string[1].isLowSurrogate()) 2 else 1
}
