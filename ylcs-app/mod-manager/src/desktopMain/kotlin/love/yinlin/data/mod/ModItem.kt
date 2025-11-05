package love.yinlin.data.mod

import androidx.compose.runtime.Stable
import kotlinx.io.files.Path

@Stable
data class ModItem(
    val id: String,
    val name: String,
    val path: Path,
    val enabled: Boolean = true,
    val shown: Boolean = true,
    val selected: Boolean = false,
)