package androidx.compose.ui.draganddrop

import androidx.compose.ui.geometry.Offset

actual class DragAndDropEvent(val transferData: DragAndDropTransferData, internal val position: Offset)

internal actual val DragAndDropEvent.positionInRoot: Offset get() = position
