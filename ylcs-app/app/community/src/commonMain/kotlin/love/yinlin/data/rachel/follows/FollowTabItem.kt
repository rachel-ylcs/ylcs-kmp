package love.yinlin.data.rachel.follows

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
enum class FollowTabItem(val title: String) {
    Follows("关注"), Followers("粉丝"), BlockUsers("黑名单");
}