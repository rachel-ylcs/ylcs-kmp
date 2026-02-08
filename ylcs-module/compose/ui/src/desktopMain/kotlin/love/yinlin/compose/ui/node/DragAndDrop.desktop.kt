package love.yinlin.compose.ui.node

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import kotlinx.io.files.Path
import java.awt.datatransfer.DataFlavor
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.dragAndDrop(
    enabled: Boolean,
    flag: DragFlag,
    onDrop: (DropResult) -> Unit
): Modifier = composed {
    if (enabled) {
        val onDropFunc by rememberUpdatedState(onDrop)

        val target = remember {
            object : DragAndDropTarget {
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val transferable = event.awtTransferable
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        val list = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                        onDropFunc(DropResult.File(list.map { Path((it as File).absolutePath) }))
                        return true
                    }
                    else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        val text = transferable.getTransferData(DataFlavor.stringFlavor) as String
                        onDropFunc(DropResult.Text(text))
                        return true
                    }
                    return false
                }
            }
        }

        dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                val transferable = event.awtTransferable
                val acceptText = transferable.isDataFlavorSupported(DataFlavor.stringFlavor)
                val acceptFile = transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                (acceptText && (flag and DragFlag.Text == DragFlag.Text)) || (acceptFile && (flag and DragFlag.File == DragFlag.File))
            },
            target = target
        )
    } else this
}