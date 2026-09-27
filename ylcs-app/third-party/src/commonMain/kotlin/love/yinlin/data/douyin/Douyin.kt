package love.yinlin.data.douyin

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedMessage

@Stable
@Serializable
data class Douyin(
    override val id: String, // ID
    override val user: DouyinUserInfo, // 用户信息
    override val time: LocalDateTime, // 时间
    override val location: String, // 定位
    override val title: String, // 标题
    override val content: String, // 内容
    override val data: DouyinData, // 数据
    override val medias: List<DouyinMedia>, // 图片集
) : UnifiedMessage {
    override fun compareTo(other: UnifiedMessage): Int = this.time.compareTo(other.time)
}