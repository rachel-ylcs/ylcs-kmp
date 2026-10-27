package androidx.compose.foundation.content

import androidx.compose.runtime.Stable

/** Native transfer formats use MIME types; unsupported formats are never advertised. */
@Stable
actual class MediaType actual constructor(actual val representation: String) {
    actual companion object {
        actual val Text = MediaType("text/*")
        actual val PlainText = MediaType("text/plain")
        actual val HtmlText = MediaType("text/html")
        actual val Image = MediaType("image/*")
        actual val All = MediaType("*/*")
    }

    override fun equals(other: Any?): Boolean = other is MediaType && representation.equals(other.representation, ignoreCase = true)
    override fun hashCode(): Int = representation.lowercase().hashCode()
    override fun toString(): String = "MediaType($representation)"
}
