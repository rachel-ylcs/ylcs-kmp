package love.yinlin.data.weibo

import kotlinx.datetime.LocalDate
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import love.yinlin.data.common.Picture

data class Weibo(
	val id: String, // ID
	val info: WeiboUserInfo, // 用户信息
	val time: String, // 时间
	val location: String, // 定位
	val text: String, // 内容
	val commentNum: Int, // 评论数
	val likeNum: Int, // 点赞数
	val repostNum: Int, // 转发数
	val pictures: List<Picture>, // 图片集
) : Comparable<Weibo> {
	override fun compareTo(other: Weibo): Int = try {
		val instant1 = TimeOutputFormat.parse(time).toInstantUsingOffset()
		val instant2 = TimeOutputFormat.parse(other.time).toInstantUsingOffset()
		instant1.compareTo(instant2)
	}
	catch (_: Exception) { 0 }

	companion object {
		private val TimeInputFormat = DateTimeComponents.Format {
			dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
			char(' ')
			monthName(MonthNames.ENGLISH_ABBREVIATED)
			char(' ')
			dayOfMonth()
			char(' ')
			hour()
			char(':')
			minute()
			char(':')
			second()
			char(' ')
			offset(UtcOffset.Formats.FOUR_DIGITS)
			char(' ')
			year()
		}

		private val TimeOutputFormat = DateTimeComponents.Format {
			date(LocalDate.Formats.ISO)
			char(' ')
			hour()
			char(':')
			minute()
			char(':')
			second()
		}

		fun formatTime(time: String): String = TimeOutputFormat.format(TimeInputFormat.parse(time))
	}
}