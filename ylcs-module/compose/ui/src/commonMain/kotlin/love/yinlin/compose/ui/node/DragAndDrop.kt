package love.yinlin.compose.ui.node

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlinx.io.files.Path

@Stable
class DragFlag private constructor(val value: Int) {
    infix fun and(other: DragFlag): DragFlag = DragFlag(value and other.value)
    infix fun or(other: DragFlag): DragFlag = DragFlag(value or other.value)

    override fun equals(other: Any?): Boolean = (other as? DragFlag)?.value == value
    override fun hashCode(): Int = value

    companion object {
        val File = DragFlag(1)
        val Text = DragFlag(2)
    }
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
    flag: DragFlag,
    onDrop: (DropResult) -> Unit
): Modifier