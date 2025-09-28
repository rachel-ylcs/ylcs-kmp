package love.yinlin.data.rachel.follows

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class FollowInfo(
    val fid: Long, // [ID]
    val uid: Int, // [用户ID]
    val name: String, // [用户昵称]
    val ts: String, // [关注时间]
    val score: Int, // [关系分]
)