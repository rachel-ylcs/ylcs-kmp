package love.yinlin.data.radio

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class RadioAudio(
    val id: String,
    val url: String, // 链接
    val br: Long, // 波特率
    val size: Long, // 大小
    val type: String, // 后缀名
)