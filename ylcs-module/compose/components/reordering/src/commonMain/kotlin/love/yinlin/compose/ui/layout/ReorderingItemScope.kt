package love.yinlin.compose.ui.layout

import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import love.yinlin.extension.cleaning

@Stable
class ReorderingItemScope internal constructor(private val state: ReorderingState, private val key: Any) {
    private class Handle {
        var coordinates: LayoutCoordinates? = null
    }

    internal var coordinates: LayoutCoordinates? = null

    @Composable
    fun Modifier.dragHandle(enabled: Boolean = true, longPress: Boolean = false): Modifier {
        val handle = remember { Handle() }
        return onGloballyPositioned { handle.coordinates = it }.pointerInput(state, key, enabled, longPress) {
            if (!enabled) return@pointerInput
            var token: Any? = null
            val start: (Offset) -> Unit = {
                val itemCoordinates = coordinates
                val handleCoordinates = handle.coordinates
                if (itemCoordinates?.isAttached == true && handleCoordinates?.isAttached == true && key in state.keys) {
                    val center = Offset(handleCoordinates.size.width / 2f, handleCoordinates.size.height / 2f)
                    token = state.start(key, itemCoordinates.localPositionOf(handleCoordinates, center))
                }
            }
            val stop: () -> Unit = {
                token?.let { state.stop(it) }
                token = null
            }
            val drag: (PointerInputChange, Offset) -> Unit = { change, amount ->
                token?.let {
                    change.consume()
                    state.drag(it, amount)
                }
            }

            cleaning({
                when {
                    longPress -> detectDragGesturesAfterLongPress(start, stop, stop, drag)
                    state.dragOrientation == Orientation.Vertical -> detectVerticalDragGestures(start, stop, stop) { change, amount ->
                        drag(change, Offset(0f, amount))
                    }
                    state.dragOrientation == Orientation.Horizontal -> detectHorizontalDragGestures(start, stop, stop) { change, amount ->
                        drag(change, Offset(amount, 0f))
                    }
                    else -> detectDragGestures(start, stop, stop, drag)
                }
            }, stop)
        }
    }
}
