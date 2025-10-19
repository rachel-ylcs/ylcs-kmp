package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.io.files.Path

object DragFlag {
    const val FILE = 1
    const val TEXT = 2
}

sealed interface DropResult {
    data class File(val path: List<Path>) : DropResult
    data class Text(val text: String) : DropResult
}

@Composable
expect fun Modifier.dragAndDrop(
    enabled: Boolean,
    flag: Int,
    onDrop: (DropResult) -> Unit
): Modifier