package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable

@Stable
sealed interface DropResult {
    @Stable
    data class File(val path: List<love.yinlin.fs.File>) : DropResult
    @Stable
    data class Text(val text: String) : DropResult
}