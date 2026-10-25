package androidx.compose.foundation.text

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.intl.Locale

/** Uses Foundation's existing translated menu strings for the node's composition locale. */
@Stable
internal class NativeTextMenuLocalization(private val locale: Locale) {
    val cut: String get() = getTranslation(ContextMenuStrings.Cut, locale)
    val copy: String get() = getTranslation(ContextMenuStrings.Copy, locale)
    val paste: String get() = getTranslation(ContextMenuStrings.Paste, locale)
    val selectAll: String get() = getTranslation(ContextMenuStrings.SelectAll, locale)
}
