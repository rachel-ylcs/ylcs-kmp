package androidx.compose.ui.platform

import androidx.compose.runtime.Stable

/** Owned clipboard text and file paths; no platform clipboard handle is retained. */
@Stable
actual class ClipEntry private constructor(val plainText: String, val filePaths: List<String>, private val hasPlainText: Boolean) {
    actual val clipMetadata: ClipMetadata = ClipMetadata(when {
        hasPlainText && filePaths.isNotEmpty() -> ["text/plain", "text/uri-list"]
        hasPlainText -> ["text/plain"]
        filePaths.isNotEmpty() -> ["text/uri-list"]
        else -> []
    })

    fun getPlainText(): String = plainText

    companion object {
        fun withPlainText(text: String): ClipEntry = ClipEntry(text, [], true)
        /** Copies file paths; receiving a drop does not open files or retain OS handles. */
        fun withFiles(files: List<String>, text: String? = null): ClipEntry = ClipEntry(text ?: "", files.toList(), text != null)
    }
}
