package love.yinlin.data.rachel.topic

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.rachel.profile.UserLevel

@Stable
@Serializable
data class TopicDetails (
	val ts: String,
	val content: String,
	val pics: List<String>,
	val section: Int,
	val label: String,
	val exp: Int
) {
	val level: Int by lazy { UserLevel.level(exp) }
}