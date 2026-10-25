package androidx.compose.ui.draganddrop

import androidx.compose.runtime.Stable

/** Copied transfer data; paths identify files and are never opened by the UI backend. */
@Stable
actual class DragAndDropTransferData(val text: String? = null, val files: List<String> = [])
