package love.yinlin.data.radio

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class RadioData(
    val commentNum: Int, // 评论数
    val likeNum: Int, // 点赞数
    val repostNum: Int, // 转发数
    val playNum: Int, // 播放数
)