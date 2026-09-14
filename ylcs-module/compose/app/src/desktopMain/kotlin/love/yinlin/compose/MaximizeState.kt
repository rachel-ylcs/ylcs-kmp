package love.yinlin.compose

import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.v2.WindowState

@Stable
@OptIn(ExperimentalComposeUiApi::class)
internal sealed interface MaximizeState {
    fun toggle(windowState: WindowState): MaximizeState

    @Stable
    data object Normal : MaximizeState {
        override fun toggle(windowState: WindowState): MaximizeState {
            if (!windowState.isInitialized) return this
            val newState = Maximized(windowState.size, windowState.position)
            windowState.requestPlacement(WindowPlacement.Maximized)
            return newState
        }
    }

    @Stable
    data class Maximized(val lastSize: DpSize, val lastPosition: DpOffset) : MaximizeState {
        override fun toggle(windowState: WindowState): MaximizeState {
            windowState.requestPlacement(WindowPlacement.Floating)
            windowState.requestBounds(DpRect(lastPosition, lastSize))
            return Normal
        }
    }
}
