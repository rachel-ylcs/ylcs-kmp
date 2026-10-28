package love.yinlin.compose.ui.node

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import love.yinlin.fs.File

@Stable
actual fun Modifier.dragDrop(
    enabled: Boolean,
    flag: DragFlag,
    onDrop: (DropResult) -> Unit
): Modifier = this.composed {
    if (enabled) {
        val currentFlag by rememberUpdatedState(flag)
        val onDropFunc by rememberUpdatedState(onDrop)

        val target = remember {
            object : DragAndDropTarget {
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val data = event.transferData
                    val text = data.text
                    if (data.files.isNotEmpty() && (currentFlag and DragFlag.File == DragFlag.File)) {
                        onDropFunc(DropResult.File(data.files.map { File(it) }))
                        return true
                    }
                    else if (text != null && (currentFlag and DragFlag.Text == DragFlag.Text)) {
                        onDropFunc(DropResult.Text(text))
                        return true
                    }
                    return false
                }
            }
        }

        this.dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                val data = event.transferData
                (data.files.isNotEmpty() && (currentFlag and DragFlag.File == DragFlag.File)) || (data.text != null && (currentFlag and DragFlag.Text == DragFlag.Text))
            },
            target = target
        )
    }
    else this
}
