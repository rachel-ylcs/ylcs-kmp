package love.yinlin.compose.ui.node

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlinx.io.files.Path

@Stable
data object DragFlag {
    const val FILE = 1
    const val TEXT = 2
}

@Stable
sealed interface DropResult {
    @Stable
    data class File(val path: List<Path>) : DropResult
    @Stable
    data class Text(val text: String) : DropResult
}

@Composable
expect fun Modifier.dragAndDrop(
    enabled: Boolean,
    flag: Int,
    onDrop: (DropResult) -> Unit
): Modifier