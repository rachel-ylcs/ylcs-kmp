@file:Suppress("DEPRECATION")

package androidx.compose.ui.platform

import androidx.compose.ui.text.AnnotatedString

internal class SdlClipboard : Clipboard, ClipboardManager {
    override val nativeClipboard: NativeClipboard = NativeClipboard()

    override fun getText(): AnnotatedString? = nativeClipboard.getText()?.let(::AnnotatedString)

    override fun setText(annotatedString: AnnotatedString) = nativeClipboard.setText(annotatedString.text)

    override fun hasText(): Boolean = nativeClipboard.hasText()

    override fun getClip(): ClipEntry? = nativeClipboard.getText()?.let(ClipEntry::withPlainText)

    override fun setClip(clipEntry: ClipEntry?) = nativeClipboard.setText(clipEntry?.plainText ?: "")

    override suspend fun getClipEntry(): ClipEntry? = getClip()

    override suspend fun setClipEntry(clipEntry: ClipEntry?) = setClip(clipEntry)
}

internal actual fun createPlatformClipboardManager(): ClipboardManager = SdlClipboard()

internal actual fun createPlatformClipboard(): Clipboard = SdlClipboard()
