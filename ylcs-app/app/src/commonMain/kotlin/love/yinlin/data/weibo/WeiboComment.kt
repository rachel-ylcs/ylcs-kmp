package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import love.yinlin.compose.ui.text.RichString
import love.yinlin.data.common.Picture
import love.yinlin.extension.DateEx

@Stable
data class WeiboComment (
	val id: String, // ID
	val info: WeiboUserInfo, // 用户
	val time: LocalDateTime, // 时间
	val location: String, // 定位
	val text: RichString, // 内容
	val pic: Picture?, // 图片
	var subComments: List<WeiboSubComment> // 楼中楼
) {
	val timeString: String = DateEx.Formatter.standardDateTime.format(time) ?: ""
}