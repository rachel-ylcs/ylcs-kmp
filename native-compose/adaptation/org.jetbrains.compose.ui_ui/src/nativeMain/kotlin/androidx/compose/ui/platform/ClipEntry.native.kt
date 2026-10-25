package androidx.compose.ui.platform

import androidx.compose.runtime.Stable

/** An owned UTF-8 text clipboard entry; no platform clipboard handle is retained. */
@Stable
actual class ClipEntry private constructor(val plainText: String) {
    actual val clipMetadata: ClipMetadata = ClipMetadata(["text/plain"])

    fun getPlainText(): String = plainText

    companion object {
        fun withPlainText(text: String): ClipEntry = ClipEntry(text)
    }
}
