package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedMessage
import love.yinlin.data.information.UnifiedPicture

@Stable
@Serializable
data class Weibo(
    override val id: String, // ID
    override val user: WeiboUserInfo, // 用户信息
    override val time: LocalDateTime, // 时间
    override val location: String, // 定位
    override val content: String, // 内容
    override val data: WeiboData, // 数据
    override val pictures: List<UnifiedPicture>, // 图片集
) : UnifiedMessage {
    override val title: String = "" // 微博暂不支持标题

    override fun compareTo(other: UnifiedMessage): Int = this.time.compareTo(other.time)
}