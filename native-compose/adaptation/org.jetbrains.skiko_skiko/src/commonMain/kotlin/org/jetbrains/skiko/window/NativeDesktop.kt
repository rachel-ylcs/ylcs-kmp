package org.jetbrains.skiko.window

/** Main-thread desktop services. Clipboard access requires an initialized Native window host. */
expect object NativeDesktop {
    fun clipboardText(): String?
    fun setClipboardText(text: String)
    fun hasClipboardText(): Boolean
    /** Opens a URI with the OS handler; returns false when no handler can open it. */
    fun openUri(uri: String): Boolean
}
