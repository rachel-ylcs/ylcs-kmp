package love.yinlin.data.bilibili

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedData

@Stable
@Serializable
data class BilibiliData(
    val commentNum: Int, // 评论数
    val likeNum: Int, // 点赞数
    val repostNum: Int, // 转发数
    val danmakuNum: String? = null, // 弹幕数
    val playNum: String? = null, // 播放数
) : UnifiedData