@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package androidx.compose.ui.text.intl

import kotlinx.cinterop.toKString
import platform.posix.getenv

internal actual fun createPlatformLocaleDelegate(): PlatformLocaleDelegate = object : PlatformLocaleDelegate {
    override val current: LocaleList get() {
        val override = localeEnvironment("LC_ALL")
        val fallback = override ?: localeEnvironment("LC_MESSAGES") ?: localeEnvironment("LANG") ?: "C"
        val preferences = if (override != null || fallback.substringBefore('.') in ["C", "POSIX"]) null else localeEnvironment("LANGUAGE")
        val identifiers: List<String> = preferences?.split(':')?.filter(String::isNotBlank)?.ifEmpty { [fallback] } ?: [fallback]
        return LocaleList(identifiers.map(::Locale))
    }
}

private fun localeEnvironment(name: String): String? = getenv(name)?.toKString()?.takeIf(String::isNotBlank)
