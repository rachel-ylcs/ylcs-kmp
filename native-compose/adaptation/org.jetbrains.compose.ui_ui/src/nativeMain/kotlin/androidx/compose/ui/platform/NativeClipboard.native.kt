@file:Suppress("DEPRECATION")

package androidx.compose.ui.platform

import org.jetbrains.skiko.window.NativeDesktop

actual class NativeClipboard internal constructor() {
    fun getText(): String? = NativeDesktop.clipboardText()
    fun setText(text: String) = NativeDesktop.setClipboardText(text)
    fun hasText(): Boolean = NativeDesktop.hasClipboardText()
}
