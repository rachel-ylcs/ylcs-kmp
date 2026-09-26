package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedData

@Stable
@Serializable
data class WeiboData(
    val commentNum: Int, // 评论数
    val likeNum: Int, // 点赞数
    val repostNum: Int, // 转发数
) : UnifiedData