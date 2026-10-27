@file:OptIn(ExperimentalForeignApi::class)

package androidx.compose.ui.text.intl

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKStringFromUtf16
import platform.windows.GetLastError
import platform.windows.GetUserDefaultLocaleName
import platform.windows.LOCALE_NAME_MAX_LENGTH

internal actual fun createPlatformLocaleDelegate(): PlatformLocaleDelegate = object : PlatformLocaleDelegate {
    @Deprecated("This method of accessing locale isn't backed by snapshot state, meaning that updates to the locale won't notify reading this API.")
    override val current: LocaleList get() = memScoped {
        val buffer = allocArray<UShortVar>(LOCALE_NAME_MAX_LENGTH)
        check(GetUserDefaultLocaleName(buffer, LOCALE_NAME_MAX_LENGTH) > 0) { "Cannot read the Windows user locale: ${GetLastError()}" }
        LocaleList(Locale(buffer.toKStringFromUtf16()))
    }
}
