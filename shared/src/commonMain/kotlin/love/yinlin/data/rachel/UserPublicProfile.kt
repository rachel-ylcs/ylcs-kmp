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
	@Stable
	val level: Int get() = UserLevel.level(coin)

	@Stable
	val avatarPath: String get() = "${APIConfig.URL}/${ServerRes.Users.User(uid).avatar}"

	@Stable
	val wallPath: String get() = "${APIConfig.URL}/${ServerRes.Users.User(uid).wall}"
}