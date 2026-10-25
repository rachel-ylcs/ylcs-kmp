package org.jetbrains.skiko.window

import kotlinx.cinterop.toKString
import org.jetbrains.skiko.internal.sdl.*

actual object NativeDesktop {
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
