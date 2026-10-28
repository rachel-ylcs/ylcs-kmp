package love.yinlin.compose.ui.node

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.domDataTransferOrNull
import love.yinlin.data.MimeType
import love.yinlin.extension.asArray
import love.yinlin.extension.cast
import love.yinlin.fs.File
import org.w3c.files.get
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
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
                    val transferData = event.transferData?.domDataTransferOrNull
                    if (transferData != null) {
                        val types = transferData.types.asArray { s: JsString -> s.cast.lowercase() }
                        if ("files" in types && (currentFlag and DragFlag.File == DragFlag.File)) {
                            val files = transferData.files
                            val len = files.length
                            val paths: MutableList<File> = []
                            for (i in 0 ..< len) paths += File(files[i]?.name ?: "")
                            onDropFunc(DropResult.File(paths))
                            return true
                        }
                        else if (MimeType.TEXT in types && (currentFlag and DragFlag.Text == DragFlag.Text)) {
                            onDropFunc(DropResult.Text(transferData.getData(MimeType.TEXT)))
                            return true
                        }
                        return false
                    }
                    return false
                }
            }
        }

        this.dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                val types = event.transferData?.domDataTransferOrNull?.types?.asArray { s: JsString -> s.cast.lowercase() }
                types?.let {
                    val acceptText = MimeType.TEXT in types
                    val acceptFile = "files" in types
                    (acceptText && (currentFlag and DragFlag.Text == DragFlag.Text)) || (acceptFile && (currentFlag and DragFlag.File == DragFlag.File))
                } ?: false
            },
            target = target
        )
    }
    else this
}
