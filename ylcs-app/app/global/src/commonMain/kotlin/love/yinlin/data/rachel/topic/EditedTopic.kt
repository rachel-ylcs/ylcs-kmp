package love.yinlin.data.rachel.topic

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class EditedTopic(
    val title: String,
    val content: String,
    val section: Int,
    val pics: List<String>
)