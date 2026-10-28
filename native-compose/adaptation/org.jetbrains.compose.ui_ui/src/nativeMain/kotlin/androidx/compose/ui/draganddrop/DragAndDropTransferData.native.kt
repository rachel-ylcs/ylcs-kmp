package androidx.compose.ui.draganddrop

import androidx.compose.runtime.Stable

@Stable
actual class DragAndDropTransferData private constructor(val text: String?, val files: List<String>, val hasFiles: Boolean, val hasText: Boolean) {
    constructor(text: String? = null, files: List<String> = []) : this(text, files, files.isNotEmpty(), text != null)
    internal constructor(hasFiles: Boolean, hasText: Boolean) : this(null, [], hasFiles, hasText)
}
