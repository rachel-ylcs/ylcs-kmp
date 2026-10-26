package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme
import org.jetbrains.skiko.internal.sdl.*
import org.jetbrains.skiko.internal.sdl.SDL_SystemTheme.*
import kotlin.concurrent.Volatile

actual object NativeDesktop {
    @Volatile
    private var theme: SystemTheme? = null
    @Volatile
    private var locales: List<String> = []

    actual fun systemTheme(): SystemTheme {
        if (SDL_IsMainThread() && SDL_WasInit(SDL_INIT_VIDEO) != 0u) {
            theme = when (SDL_GetSystemTheme()) {
                SDL_SYSTEM_THEME_DARK -> SystemTheme.DARK
                SDL_SYSTEM_THEME_LIGHT -> SystemTheme.LIGHT
                else -> currentSystemTheme
            }
        }
        return theme ?: currentSystemTheme
    }

    actual fun preferredLocales(): List<String> {
        if (SDL_IsMainThread()) memScoped {
            val count = alloc<IntVar>()
            val data = SDL_GetPreferredLocales(count.ptr)
            if (data != null) {
                try {
                    locales = buildList {
                        for (index in 0 ..< count.value) {
                            val locale = data[index]?.pointed ?: continue
                            val language = locale.language?.toKString()?.replace('_', '-') ?: continue
                            if (language.isBlank()) continue
                            val country = locale.country?.toKString()
                            add(if (country.isNullOrBlank()) language else "$language-$country")
                        }
                    }.distinct()
                }
                finally { SDL_free(data) }
            }
        }
        return locales
    }

    actual fun clipboardText(): String? {
        val data = SDL_GetClipboardText() ?: error(SDL_GetError()?.toKString() ?: "Unable to read clipboard")
        try {
            return data.toKString().ifEmpty { null }
        }
        finally { SDL_free(data) }
    }

    actual fun setClipboardText(text: String) {
        require('\u0000' !in text) { "Clipboard text contains a NUL character" }
        check(SDL_SetClipboardText(text)) { SDL_GetError()?.toKString() ?: "Unable to write clipboard" }
    }

    actual fun hasClipboardText(): Boolean = SDL_HasClipboardText()

    actual fun openUri(uri: String): Boolean {
        require(uri.isNotBlank() && '\u0000' !in uri) { "URI is empty or contains a NUL character" }
        return SDL_OpenURL(uri)
    }
}
