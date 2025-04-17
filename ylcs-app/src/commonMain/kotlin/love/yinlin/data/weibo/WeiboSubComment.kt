package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import love.yinlin.extension.DateEx
import love.yinlin.ui.component.text.RichString

@Stable
data class WeiboSubComment(
	val id: String, // ID
	val info: WeiboUserInfo, // 用户
	val time: LocalDateTime, // 时间
	val location: String, // 定位
	val text: RichString, // 内容
) {
	val timeString: String = DateEx.Formatter.standardDateTime.format(time) ?: ""
}