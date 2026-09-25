package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import love.yinlin.extension.DateEx

@Stable
@Serializable
data class WeiboSubComment(
    val id: String, // ID
    val info: WeiboUserInfo, // 用户
    val time: LocalDateTime, // 时间
    val location: String, // 定位
    val content: String, // 内容
) {
	val timeString: String = DateEx.Formatter.standardDateTime.format(time) ?: ""
}