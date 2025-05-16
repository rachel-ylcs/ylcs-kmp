package love.yinlin.data.rachel.profile

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.Local
import love.yinlin.api.ServerRes
import love.yinlin.data.rachel.follows.FollowStatus

@Stable
@Serializable
data class UserPublicProfile(
	val uid: Int,
	val name: String,
	val signature: String,
	val label: String,
	val coin: Int,
	val follows: Int,
	val followers: Int,
	val status: FollowStatus,
) {
	val level: Int by lazy { UserLevel.level(coin) }

	val avatarPath: String by lazy { "${Local.ClientUrl}/${ServerRes.Users.User(uid).avatar}" }

	val wallPath: String by lazy { "${Local.ClientUrl}/${ServerRes.Users.User(uid).wall}" }
}