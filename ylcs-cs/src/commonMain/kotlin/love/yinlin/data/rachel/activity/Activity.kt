package love.yinlin.data.rachel.activity

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.Local
import love.yinlin.api.ServerRes
import love.yinlin.extension.DateEx

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
) : Comparable<Activity> {
	override fun compareTo(other: Activity): Int {
		val today = DateEx.Today.toEpochDays()
		val day1 = (this.ts?.let { DateEx.Formatter.standardDate.parse(it) }?.toEpochDays() ?: Long.MAX_VALUE) - today
		val day2 = (other.ts?.let { DateEx.Formatter.standardDate.parse(it) }?.toEpochDays() ?: Long.MAX_VALUE) - today
		return if (day1 < 0L) {
			if (day2 < 0L) (day2 - day1).toInt() else 1
		}
		else {
			if (day2 < 0L) -1 else (day1 - day2).toInt()
		}
	}

	val picPath: String? by lazy { pic?.let { "${Local.API_BASE_URL}/${ServerRes.Activity.activity(it)}" } }

	fun picPath(key: String): String = "${Local.API_BASE_URL}/${ServerRes.Activity.activity(key)}"
}