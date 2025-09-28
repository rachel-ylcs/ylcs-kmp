package love.yinlin.data.rachel.follows

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
enum class FollowStatus {
    UNAUTHORIZE, // [未登录] -> NULL | NULL
    SELF, // [自己] -> NULL | TOPICS
    FOLLOW, // [已关注] -> 取关 | TOPICS
    UNFOLLOW, // [未关注] -> 关注, 拉黑 | TOPICS
    BLOCK, // [拉黑] -> NULL | TOPICS
    BLOCKED, // [被拉黑] -> 拉黑 | NULL
    BIDIRECTIONAL_BLOCK; // [双向拉黑] -> NULL | NULL

    val canShowTopics: Boolean get() = this == SELF || this == FOLLOW || this == UNFOLLOW || this == BLOCK
    val isFollowed: Boolean? get() = if (this == FOLLOW) true else if (this == UNFOLLOW) false else null
    val canBlock: Boolean get() = this == UNFOLLOW || this == BLOCKED
}