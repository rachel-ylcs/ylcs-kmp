package love.yinlin.data.rachel.activity

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class ActivityPhoto(
    val cover: String? = null, // [活动封面]
    val seat: String? = null, // [座位图]
    val posters: List<String> = emptyList(), // [海报]
)