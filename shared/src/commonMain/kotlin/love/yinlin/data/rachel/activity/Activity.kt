package love.yinlin.data.rachel.activity

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.Local
import love.yinlin.api.ServerRes

@Stable
@Serializable
data class Activity(
	val aid: Int, // [活动 ID]
	val ts: String?, // [活动时间]
	val title: String?, // [活动标题]
	val content: String, // [活动内容]
	val pic: String?, // [活动大屏]
	val pics: List<String>, // [活动海报]
	val showstart: String?, // [秀动链接]
	val damai: String?, // [大麦链接]
	val maoyan: String?, // [猫眼链接]
	val link: String? // [活动链接]
) {
	val picPath: String? by lazy { pic?.let { "${Local.ClientUrl}/${ServerRes.Activity.activity(it)}" } }

	fun picPath(key: String): String = "${Local.ClientUrl}/${ServerRes.Activity.activity(key)}"
}