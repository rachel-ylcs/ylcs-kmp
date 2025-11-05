package love.yinlin.data.mod

import androidx.compose.runtime.Stable

@Stable
data class ModItem(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val shown: Boolean = true,
    val selected: Boolean = false,
)