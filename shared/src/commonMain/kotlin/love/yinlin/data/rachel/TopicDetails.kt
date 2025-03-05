package love.yinlin.data.rachel

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class TopicDetails (
	val ts: String,
	val content: String,
	val pics: List<String>,
	val section: Int,
	val label: String,
	val coin: Int
) {
	val level: Int by lazy { UserLevel.level(coin) }
}