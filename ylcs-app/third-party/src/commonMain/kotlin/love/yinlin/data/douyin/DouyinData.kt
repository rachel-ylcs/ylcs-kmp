package love.yinlin.data.douyin

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedData

@Stable
@Serializable
data class DouyinData(
    val recommendNum: Int, // 推荐数
    val commentNum: Int, // 评论数
    val likeNum: Int, // 点赞数
    val admireNum: Int, // 打赏数
    val repostNum: Int, // 转发数
    val collectNum: Int, // 收藏数
) : UnifiedData