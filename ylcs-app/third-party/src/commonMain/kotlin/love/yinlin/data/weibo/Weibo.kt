package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedMessage
import love.yinlin.data.information.UnifiedPicture
import love.yinlin.extension.DateEx

@Stable
@Serializable
data class Weibo(
    override val id: String, // ID
    override val user: WeiboUserInfo, // 用户信息
    override val time: LocalDateTime, // 时间
    override val location: String, // 定位
    override val title: String, // 标题
    override val content: String, // 内容
    override val commentNum: Int, // 评论数
    override val likeNum: Int, // 点赞数
    override val repostNum: Int, // 转发数
    override val pictures: List<UnifiedPicture>, // 图片集
) : UnifiedMessage {
    override fun compareTo(other: UnifiedMessage): Int = this.time.compareTo(other.time)

    val timeString: String = DateEx.Formatter.standardDateTime.format(time) ?: ""
}