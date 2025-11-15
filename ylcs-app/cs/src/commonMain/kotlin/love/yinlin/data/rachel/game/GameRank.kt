package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.Local
import love.yinlin.api.ServerRes

@Stable
@Serializable
data class GameRank(
    val uid: Int,
    val name: String,
    val cnt: Int
) {
    val avatarPath by lazy { ServerRes.Users.User(uid).avatar }
}