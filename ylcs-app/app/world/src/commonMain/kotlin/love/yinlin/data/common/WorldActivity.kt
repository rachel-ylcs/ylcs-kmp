package love.yinlin.data.common

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.DrawableResource

@Stable
data class WorldActivity(
    val id: String,
    val title: String,
    val subtitle: String,
    val cover: DrawableResource,
    val keywords: List<String>,
    val onClick: () -> Unit,
)
