package androidx.compose.ui.platform

import androidx.compose.runtime.Stable

/** Describes formats without exposing clipboard contents or fetching data from the OS. */
@Stable
actual class ClipMetadata internal constructor(val mimeTypes: Set<String>) {
    fun hasMimeType(mimeType: String): Boolean = mimeTypes.any {
        it == mimeType || mimeType == "*/*" || (mimeType.endsWith("/*") && it.substringBefore('/') == mimeType.substringBefore('/'))
    }
}
