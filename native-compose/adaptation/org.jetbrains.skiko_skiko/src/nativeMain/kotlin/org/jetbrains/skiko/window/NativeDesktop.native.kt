package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme
import org.jetbrains.skiko.hostOs
import org.jetbrains.skiko.internal.sdl.*
import org.jetbrains.skiko.internal.sdl.SDL_SystemTheme.*
import kotlin.concurrent.Volatile

/** Main-thread desktop services. Clipboard access requires an initialized Native window host. */
object NativeDesktop {
    @Volatile
    private var theme: SystemTheme? = null
    @Volatile
    private var locales: List<String> = []

    fun systemTheme(): SystemTheme {
        if (SDL_IsMainThread() && SDL_WasInit(SDL_INIT_VIDEO) != 0u) {
            theme = when (SDL_GetSystemTheme()) {
                SDL_SYSTEM_THEME_DARK -> SystemTheme.DARK
                SDL_SYSTEM_THEME_LIGHT -> SystemTheme.LIGHT
                else -> currentSystemTheme
            }
        }
        return theme ?: currentSystemTheme
    }

    fun preferredLocales(): List<String> {
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

    fun clipboardText(): String? {
        val data = SDL_GetClipboardText() ?: error(SDL_GetError()?.toKString() ?: "Unable to read clipboard")
        try {
            return data.toKString().ifEmpty { null }
        }
        finally { SDL_free(data) }
    }

    fun setClipboardText(text: String) {
        require('\u0000' !in text) { "Clipboard text contains a NUL character" }
        check(SDL_SetClipboardText(text)) { SDL_GetError()?.toKString() ?: "Unable to write clipboard" }
    }

    fun hasClipboardText(): Boolean = SDL_HasClipboardText()

    /** Open a filesystem path with the system application. Call on the window host thread. */
    fun openFile(path: String): Boolean = openNativeFile(path)

    fun openUri(uri: String): Boolean {
        require(uri.isNotBlank() && '\u0000' !in uri) { "URI is empty or contains a NUL character" }
        // SDL passes this straight to ShellExecuteW on Windows. Its file-URL handling
        // does not reliably decode UTF-8 escapes, so open the decoded Unicode path.
        // Other schemes and platforms must retain their URI escaping.
        if (hostOs.isWindows && uri.startsWith("file://", ignoreCase = true)) {
            val location = uri.substring(7).substringBefore('#').substringBefore('?')
            require(location.isNotEmpty()) { "File URI path is empty" }
            val path = when {
                location.startsWith('/') || location.getOrNull(1) == ':' -> location
                location.startsWith("localhost/", ignoreCase = true) -> location.substring(9)
                else -> "//$location"
            }
            val decoded = decodeFileUriPath(path)
            val windowsPath = if (decoded.startsWith('/') && decoded.getOrNull(2) == ':') decoded.substring(1) else decoded
            require(windowsPath.isNotBlank() && '\u0000' !in windowsPath) { "File URI path is empty or contains a NUL character" }
            return SDL_OpenURL(windowsPath.replace('/', '\\'))
        }
        return SDL_OpenURL(uri)
    }

    private fun decodeFileUriPath(path: String): String {
        val encoded = path.encodeToByteArray()
        val decoded = ByteArray(encoded.size)
        var input = 0
        var output = 0
        while (input < encoded.size) {
            if (encoded[input] == '%'.code.toByte()) {
                require(input + 2 < encoded.size) { "Incomplete escape in file URI" }
                val high = encoded[input + 1].toInt().toChar().digitToIntOrNull(16)
                val low = encoded[input + 2].toInt().toChar().digitToIntOrNull(16)
                require(high != null && low != null) { "Invalid escape in file URI" }
                decoded[output++] = ((high shl 4) or low).toByte()
                input += 3
            }
            else decoded[output++] = encoded[input++]
        }
        return decoded.decodeToString(endIndex = output, throwOnInvalidSequence = true)
    }
}

internal expect fun openNativeFile(path: String): Boolean
