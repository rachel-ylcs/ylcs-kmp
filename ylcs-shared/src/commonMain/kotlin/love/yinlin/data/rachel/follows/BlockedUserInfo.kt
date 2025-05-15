package love.yinlin.data.rachel.follows

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class BlockedUserInfo(
    val fid: Long, // [ID]
    val uid: Int, // [用户ID]
    val name: String, // [用户昵称]
)