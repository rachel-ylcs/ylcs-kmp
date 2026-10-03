package love.yinlin.data.radio

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedMessage

@Stable
@Serializable
data class Radio(
    override val id: String, // ID
    override val user: RadioUserInfo, // 用户信息
    override val time: LocalDateTime, // 时间
    override val location: String, // 定位
    override val title: String, // 标题
    override val content: String, // 内容
    override val data: RadioData, // 数据
    override val medias: List<RadioMedia>, // 图片集
    val duration: Long, // 时长
    val audioId: String, // 音频链接
) : UnifiedMessage {
    override fun compareTo(other: UnifiedMessage): Int = this.time.compareTo(other.time)
}