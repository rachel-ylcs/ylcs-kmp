package love.yinlin.compose.ui.window

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.WindowScope
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
            window.removeMouseMotionListener(dragListener)
            window.removeMouseListener(this)
        }
    }

    fun onDragStarted() {
        dragStartPoint = current ?: return
        val location = window.location
        windowLocationAtDragStart = IntOffset(location.x, location.y)

        window.addMouseListener(removeListener)
        window.addMouseMotionListener(dragListener)
    }
}

@Composable
fun WindowScope.DragArea(
    enabled: Boolean = true,
    onLongClick: ((Offset) -> Unit)? = null,
    onDoubleClick: ((Offset) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val handler = remember { DragHandler(window) }

    Box(modifier = Modifier.pointerInput(handler, enabled) {
        detectTapGestures(
            onPress = { if (enabled) handler.onDragStarted() },
            onLongPress = onLongClick,
            onDoubleTap = onDoubleClick
        )
    }) {
        content()
    }
}