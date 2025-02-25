package love.yinlin.data.rachel

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class Activity(
	val ts: String, // [活动时间]
	val title: String, // [活动标题]
)