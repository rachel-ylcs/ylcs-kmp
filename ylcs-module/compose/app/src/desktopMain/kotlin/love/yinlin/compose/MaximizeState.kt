package love.yinlin.compose

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState

@Stable
internal sealed interface MaximizeState {
    fun toggle(windowState: WindowState): MaximizeState

    @Stable
    data object Normal : MaximizeState {
        override fun toggle(windowState: WindowState): MaximizeState {
            val newState = Maximized(windowState.size, windowState.position)
            windowState.placement = WindowPlacement.Maximized
            return newState
        }
    }

    @Stable
    data class Maximized(val lastSize: DpSize, val lastPosition: WindowPosition) : MaximizeState {
        override fun toggle(windowState: WindowState): MaximizeState {
            windowState.placement = WindowPlacement.Floating
            windowState.size = lastSize
            windowState.position = lastPosition
            return Normal
        }
    }
}