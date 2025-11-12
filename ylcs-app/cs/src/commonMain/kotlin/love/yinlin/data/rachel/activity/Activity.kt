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
	val tsInfo: String?, // [活动时间补充]
	val location: String?, // [活动地点]
	val shortTitle: String?, // [活动短标题]
	val title: String?, // [活动标题]
	val content: String?, // [活动内容]
	val price: List<ActivityPrice>, // [活动票价]
	val saleTime: List<String>, // [开票时间]
	val lineup: List<String>, // [演出阵容]
	val photo: ActivityPhoto, // [活动照片]
	val link: ActivityLink, // [活动链接]
	val playlist: List<String>, // [歌单]
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

	val coverPath: String? by lazy { photo.cover?.let { "${Local.API_BASE_URL}/${ServerRes.Activity.activity(it)}" } }

	val seatPath: String? by lazy { photo.seat?.let { "${Local.API_BASE_URL}/${ServerRes.Activity.activity(it)}" } }

	fun posterPath(key: String): String = "${Local.API_BASE_URL}/${ServerRes.Activity.activity(key)}"
}