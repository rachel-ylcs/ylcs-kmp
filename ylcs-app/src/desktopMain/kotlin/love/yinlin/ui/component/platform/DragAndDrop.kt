package love.yinlin.ui.component.platform

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import kotlinx.io.files.Path
import java.awt.datatransfer.DataFlavor
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun Modifier.dragAndDrop(
    enabled: Boolean,
    flag: Int,
    onDrop: (DropResult) -> Unit
): Modifier = if (enabled) this.dragAndDropTarget(
    shouldStartDragAndDrop = {
        val transferable = it.awtTransferable
        val acceptText = transferable.isDataFlavorSupported(DataFlavor.stringFlavor)
        val acceptFile = transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
        (acceptText && (flag and DragFlag.TEXT == DragFlag.TEXT)) ||
                (acceptFile && (flag and DragFlag.FILE == DragFlag.FILE))
    },
    target = remember(onDrop) { object : DragAndDropTarget {
        override fun onDrop(event: DragAndDropEvent): Boolean {
            val transferable = event.awtTransferable
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                val list = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                onDrop(DropResult.File(list.map { Path((it as File).absolutePath) }))
            }
            else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                val text = transferable.getTransferData(DataFlavor.stringFlavor) as String
                onDrop(DropResult.Text(text))
            }
            return false
        }
    } }
) else this