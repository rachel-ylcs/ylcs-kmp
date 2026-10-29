package love.yinlin.compose.ui.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

@Composable
actual fun WindowScope.DragArea(enabled: Boolean, onLongClick: ((Offset) -> Unit)?, onDoubleClick: ((Offset) -> Unit)?, modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier then WindowDragAreaElement(this, enabled, onLongClick, onDoubleClick), propagateMinConstraints = true, content = { content() })
}

/** 设置区域是否允许移动所属窗口，子组件仍按 Compose 的事件消费规则优先处理。 */
@Stable
fun Modifier.windowDragArea(window: WindowScope, draggable: Boolean = true): Modifier = this then WindowDragAreaElement(window, draggable, null, null)

private class WindowDragAreaNode(var window: WindowScope, var draggable: Boolean, var onLongClick: ((Offset) -> Unit)?, var onDoubleClick: ((Offset) -> Unit)?) : DelegatingNode() {
    private var registration: AutoCloseable? = null
    private val input = delegate(SuspendingPointerInputModifierNode {
        val inputScope = this
        detectTapGestures(
            onPress = {
                if (draggable) inputScope.awaitPointerEventScope {
                    if (!currentEvent.buttons.isPrimaryPressed) return@awaitPointerEventScope
                    val down = currentEvent.changes.firstOrNull { it.pressed } ?: return@awaitPointerEventScope
                    val change = awaitDragOrCancellation(down.id) ?: return@awaitPointerEventScope
                    if (change.pressed) {
                        change.consume()
                        window.startDrag()
                    }
                }
            },
            onLongPress = if (onLongClick != null) { offset -> onLongClick?.invoke(offset) } else null,
            onDoubleTap = if (onDoubleClick != null) { offset -> onDoubleClick?.invoke(offset) } else null,
        )
    })

    override fun onAttach() {
        // Keep native border resizing, while window movement is negotiated by Compose gestures.
        registration = window.addDragArea { _, _ -> false }
    }

    override fun onDetach() {
        registration?.close()
        registration = null
    }

    fun update(window: WindowScope, draggable: Boolean, onLongClick: ((Offset) -> Unit)?, onDoubleClick: ((Offset) -> Unit)?) {
        val changedWindow = this.window !== window
        val restart = changedWindow || this.draggable != draggable || (this.onLongClick == null) != (onLongClick == null) || (this.onDoubleClick == null) != (onDoubleClick == null)
        if (changedWindow) {
            registration?.close()
            registration = null
        }
        this.window = window
        this.draggable = draggable
        this.onLongClick = onLongClick
        this.onDoubleClick = onDoubleClick
        if (changedWindow && isAttached) onAttach()
        if (restart) input.resetPointerInputHandler()
    }
}

private data class WindowDragAreaElement(val window: WindowScope, val draggable: Boolean, val onLongClick: ((Offset) -> Unit)?, val onDoubleClick: ((Offset) -> Unit)?) : ModifierNodeElement<WindowDragAreaNode>() {
    override fun create() = WindowDragAreaNode(window, draggable, onLongClick, onDoubleClick)

    override fun update(node: WindowDragAreaNode) {
        node.update(window, draggable, onLongClick, onDoubleClick)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "windowDragArea"
        properties["draggable"] = draggable
    }
}
