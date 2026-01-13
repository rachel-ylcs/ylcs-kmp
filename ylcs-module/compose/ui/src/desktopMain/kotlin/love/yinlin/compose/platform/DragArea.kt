package love.yinlin.compose.platform

import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.WindowScope

@Composable
fun WindowScope.DragArea(enabled: Boolean, content: @Composable () -> Unit) {
    if (enabled) WindowDraggableArea(content = content)
    else content()
}