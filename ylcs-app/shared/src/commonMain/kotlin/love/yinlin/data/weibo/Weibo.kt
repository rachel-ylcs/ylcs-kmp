package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import love.yinlin.data.compose.Picture
import love.yinlin.extension.DateEx
import love.yinlin.compose.ui.text.RichString

@Stable
data class Weibo(
	val id: String, // ID
	val info: WeiboUserInfo, // 用户信息
	val time: LocalDateTime, // 时间
	val location: String, // 定位
	val text: RichString, // 内容
	val commentNum: Int, // 评论数
	val likeNum: Int, // 点赞数
	val repostNum: Int, // 转发数
	val pictures: List<Picture>, // 图片集
) : Comparable<Weibo> {
	override fun compareTo(other: Weibo): Int = this.time.compareTo(other.time)

	val timeString: String = DateEx.Formatter.standardDateTime.format(time) ?: ""
}