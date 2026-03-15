package love.yinlin.data.mod

import androidx.compose.runtime.Stable
import love.yinlin.fs.File

@Stable
data class ModItem(
    val id: String,
    val name: String,
    val path: File,
    val enabled: Boolean = true,
    val shown: Boolean = true,
    val selected: Boolean = false,
)