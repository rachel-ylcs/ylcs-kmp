package org.jetbrains.skiko.window

import org.jetbrains.skiko.SystemTheme

/** Main-thread desktop services. Clipboard access requires an initialized Native window host. */
expect object NativeDesktop {
    /** Refreshes on the initialized host thread; other callers receive the last observed theme. */
    fun systemTheme(): SystemTheme
    /** Copies the OS preference order as language tags. Query on the host thread; otherwise returns the cached list. */
    fun preferredLocales(): List<String>
    fun clipboardText(): String?
    fun setClipboardText(text: String)
    fun hasClipboardText(): Boolean
    /** Opens a URI with the OS handler; returns false when no handler can open it. */
    fun openUri(uri: String): Boolean
}
