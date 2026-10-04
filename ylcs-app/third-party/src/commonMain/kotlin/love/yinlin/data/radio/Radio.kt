package love.yinlin.data.radio

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class Radio(
    val id: String, // ID
    val user: RadioUserInfo, // 用户信息
    val time: LocalDateTime, // 时间
    val title: String, // 标题
    val content: String, // 内容
    val data: RadioData, // 数据
    val pic: String, // 图片
    val duration: Long, // 时长
    val audioId: String, // 音频链接
) : Comparable<Radio> {
    override fun compareTo(other: Radio): Int = this.time.compareTo(other.time)
}