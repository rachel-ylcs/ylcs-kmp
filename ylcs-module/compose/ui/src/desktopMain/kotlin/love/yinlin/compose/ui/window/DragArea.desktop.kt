package love.yinlin.compose.ui.window

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import java.awt.MouseInfo
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter

private class DragHandler(private val window: Window) {
    var windowLocationAtDragStart: IntOffset? = null
    var dragStartPoint: IntOffset? = null

    val current: IntOffset? get() {
        val location = MouseInfo.getPointerInfo()?.location ?: return null
        return IntOffset(location.x, location.y)
    }

    val dragListener = object : MouseMotionAdapter() {
        override fun mouseDragged(event: MouseEvent) {
            val windowLocationAtDragStart = windowLocationAtDragStart ?: return
            val dragStartPoint = dragStartPoint ?: return
            val point = current ?: return
            val newLocation = windowLocationAtDragStart + (point - dragStartPoint)
            window.setLocation(newLocation.x, newLocation.y)
        }
    }

    val removeListener = object : MouseAdapter() {
        override fun mouseReleased(event: MouseEvent) {
            stop()
        }
    }

    fun onDragStarted() {
        stop()
        dragStartPoint = current ?: return
        val location = window.location
        windowLocationAtDragStart = IntOffset(location.x, location.y)

        window.addMouseListener(removeListener)
        window.addMouseMotionListener(dragListener)
    }

    fun stop() {
        window.removeMouseMotionListener(dragListener)
        window.removeMouseListener(removeListener)
        dragStartPoint = null
        windowLocationAtDragStart = null
    }
}

@Composable
actual fun WindowScope.DragArea(
    enabled: Boolean,
    onLongClick: ((Offset) -> Unit)?,
    onDoubleClick: ((Offset) -> Unit)?,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    DragAreaContent(enabled, modifier, onLongClick, onDoubleClick, content)
}

@Composable
private fun WindowScope.DragAreaContent(
    enabled: Boolean,
    modifier: Modifier,
    onLongClick: ((Offset) -> Unit)?,
    onDoubleClick: ((Offset) -> Unit)?,
    content: @Composable () -> Unit,
) {
    val handler = remember(window) { DragHandler(window) }
    val currentLongClick = rememberUpdatedState(onLongClick)
    val currentDoubleClick = rememberUpdatedState(onDoubleClick)

    DisposableEffect(handler, enabled) {
        if (!enabled) handler.stop()
        onDispose { handler.stop() }
    }

    Box(modifier = modifier.pointerInput(handler, enabled, onLongClick != null, onDoubleClick != null) {
        detectTapGestures(
            onPress = {
                if (enabled) {
                    handler.onDragStarted()
                    try { tryAwaitRelease() }
                    finally { handler.stop() }
                }
            },
            onLongPress = if (onLongClick != null) { offset -> currentLongClick.value?.invoke(offset) } else null,
            onDoubleTap = if (onDoubleClick != null) { offset -> currentDoubleClick.value?.invoke(offset) } else null,
        )
    }, propagateMinConstraints = true) {
        content()
    }
}
