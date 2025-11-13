package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.Local
import love.yinlin.api.ServerRes2

@Stable
@Serializable
data class GameRank(
    val uid: Int,
    val name: String,
    val cnt: Int
) {
    val avatarPath: String by lazy { "${Local.API_BASE_URL}/${ServerRes2.Users.User(uid).avatar}" }
}