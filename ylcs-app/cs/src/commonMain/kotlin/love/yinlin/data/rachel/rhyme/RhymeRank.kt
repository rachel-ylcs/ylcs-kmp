package love.yinlin.data.rachel.rhyme

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.api.ServerRes

@Stable
@Serializable
data class RhymeRank(
    val uid: Int,
    val name: String,
    val score: Int
) {
    val avatarPath by lazy { ServerRes.Users.User(uid).avatar }
}


