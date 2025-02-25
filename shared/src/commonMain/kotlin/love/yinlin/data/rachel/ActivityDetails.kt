package love.yinlin.data.rachel

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class ActivityDetails(
	val ts: String, // [活动时间]
	val title: String, // [活动标题]
	val content: String, // [活动内容]
	val pics: List<String>, // [活动海报]
	val showstart: String?, // [秀动唤醒链接]
	val damai: String?, // [大麦唤醒链接]
	val maoyan: String?, // [猫眼唤醒链接]
)