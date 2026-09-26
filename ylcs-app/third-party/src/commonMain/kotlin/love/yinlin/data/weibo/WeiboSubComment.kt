package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedData
import love.yinlin.data.information.UnifiedMessage
import love.yinlin.data.information.UnifiedPicture

@Stable
@Serializable
data class WeiboSubComment(
    override val id: String, // ID
    override val user: WeiboUserInfo, // 用户
    override val time: LocalDateTime, // 时间
    override val location: String, // 定位
    override val content: String, // 内容
) : UnifiedMessage {
    override val title: String = "" // 微博不支持标题
    override val data: UnifiedData? = null // 评论没有数据
    override val pictures: List<UnifiedPicture> = [] // 楼中楼不支持图片

    override fun compareTo(other: UnifiedMessage): Int = this.time.compareTo(other.time)
}