package love.yinlin.data.bilibili

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class BilibiliOfficial(
    val title: String,
    val description: String
)