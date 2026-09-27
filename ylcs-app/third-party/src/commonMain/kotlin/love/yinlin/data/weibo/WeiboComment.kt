package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedData
import love.yinlin.data.information.UnifiedMessage

@Stable
@Serializable
data class WeiboComment(
    override val id: String, // ID
    override val user: WeiboUserInfo, // 用户
    override val time: LocalDateTime, // 时间
    override val location: String, // 定位
    override val content: String, // 内容
    override val medias: List<WeiboMedia>, // 媒体
    val subComments: List<WeiboSubComment> // 楼中楼
) : UnifiedMessage {
    constructor(
        id: String,
        user: WeiboUserInfo,
        time: LocalDateTime,
        location: String,
        content: String,
        picture: WeiboMedia.Image?, // 图片
        subComments: List<WeiboSubComment>,
    ) : this(id, user, time, location, content, picture?.let(::listOf) ?: [], subComments)

    override val title: String = "" // 微博暂不支持标题
    override val data: UnifiedData? = null // 评论没有数据

    override fun compareTo(other: UnifiedMessage): Int = this.time.compareTo(other.time)
}