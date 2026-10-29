package love.yinlin.compose

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.window.windowDragArea

@Stable
fun Modifier.windowDragArea(controller: WindowController, draggable: Boolean = true): Modifier {
    return controller.windowScope?.let { windowDragArea(it, draggable) } ?: this
}
