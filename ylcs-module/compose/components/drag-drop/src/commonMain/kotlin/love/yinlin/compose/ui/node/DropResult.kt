package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable
import love.yinlin.uri.ImplicitUri

@Stable
sealed interface DropResult {
    @Stable
    data class File(val files: List<ImplicitUri>) : DropResult
    @Stable
    data class Text(val text: String) : DropResult
}
