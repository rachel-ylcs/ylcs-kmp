package love.yinlin.compose.ui.node

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.domDataTransferOrNull
import kotlinx.io.files.Path
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.ArrayCompatible
import love.yinlin.data.MimeType
import love.yinlin.extension.asArray
import love.yinlin.extension.cast
import org.w3c.dom.get
import org.w3c.files.get
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
@Composable
actual fun Modifier.dragAndDrop(
    enabled: Boolean,
    flag: DragFlag,
    onDrop: (DropResult) -> Unit
): Modifier = if (enabled) dragAndDropTarget(
    shouldStartDragAndDrop = { event ->
        val types = event.transferData?.domDataTransferOrNull?.types?.asArray { s: JsString -> s.cast.lowercase() }?.let(::ArrayCompatible)
        types?.let {
            val acceptText = MimeType.TEXT in types
            val acceptFile = "files" in types
            (acceptText && (flag and DragFlag.Text == DragFlag.Text)) ||
                    (acceptFile && (flag and DragFlag.File == DragFlag.File))
        } ?: false
    },
    target = remember(onDrop) { object : DragAndDropTarget {
        override fun onDrop(event: DragAndDropEvent): Boolean {
            val transferData = event.transferData?.domDataTransferOrNull
            if (transferData != null) {
                val types = ArrayCompatible(transferData.types.asArray { s: JsString -> s.cast.lowercase() })
                if (MimeType.TEXT in types) {
                    val items = transferData.items
                    items[0]?.getAsString { onDrop(DropResult.Text(it)) }
                    return true
                }
                else if ("files" in types) {
                    val files = transferData.files
                    val len = files.length
                    val paths = mutableListOf<Path>()
                    for (i in 0 ..< len) paths += Path(files[i]?.name ?: "")
                    onDrop(DropResult.File(paths))
                    return true
                }
                return false
            }
            return false
        }
    } }
) else this