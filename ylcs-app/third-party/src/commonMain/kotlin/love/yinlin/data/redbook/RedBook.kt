package love.yinlin.data.redbook

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedMessage

@Stable
@Serializable
data class RedBook(
    override val id: String, // ID
    override val user: RedBookUserInfo, // 用户信息
    override val time: LocalDateTime, // 时间
    override val title: String, // 内容
    override val data: RedBookData, // 数据
    override val medias: List<RedBookMedia>, // 图片集
    val xsecToken: String,
) : UnifiedMessage {
    override val content: String = "" // 没有内容
    override val location: String = "" // 没有定位

    override fun compareTo(other: UnifiedMessage): Int = this.time.compareTo(other.time)
}