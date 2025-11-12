package love.yinlin.data.rachel.activity

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.extension.DateEx

@Stable
@Serializable
data class Activity(
	val aid: Int, // [活动 ID]
	val ts: String? = null, // [活动时间]
	val tsInfo: String? = null, // [活动时间补充]
	val location: String? = null, // [活动地点]
	val shortTitle: String? = null, // [活动短标题]
	val title: String? = null, // [活动标题]
	val content: String? = null, // [活动内容]
	val price: List<ActivityPrice> = emptyList(), // [活动票价]
	val saleTime: List<String> = emptyList(), // [开票时间]
	val lineup: List<String> = emptyList(), // [演出阵容]
	val photo: ActivityPhoto = ActivityPhoto(), // [活动照片]
	val link: ActivityLink = ActivityLink(), // [活动链接]
	val playlist: List<String> = emptyList(), // [歌单]
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
}