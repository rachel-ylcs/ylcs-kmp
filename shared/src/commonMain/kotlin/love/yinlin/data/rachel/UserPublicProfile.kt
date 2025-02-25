package love.yinlin.data.rachel

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.api.APIConfig
import love.yinlin.api.ServerRes

@Stable
@Serializable
data class UserPublicProfile(
	val uid: Int,
	val name: String,
	val signature: String,
	val label: String,
	val coin: Int,
) {
	val avatarPath: String get() = "${APIConfig.URL}/${ServerRes.Users.User(uid).avatar}"
	val wallPath: String get() = "${APIConfig.URL}/${ServerRes.Users.User(uid).wall}"

	val level: Int get() = UserLevel.level(coin)
}