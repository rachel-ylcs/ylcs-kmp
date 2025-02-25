package love.yinlin.data.rachel

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.api.APIConfig
import love.yinlin.api.ServerRes

@Stable
@Serializable
data class SubComment(
	val cid: Int, // [评论 ID]
	val uid: Int, // [用户 ID]
	val ts: String, // [发布时间]
	val content: String, // [内容]
	val name: String, // [用户昵称]
	val label: String, // [用户标签]
	val coin: Int // [用户银币]
) {
	val level: Int get() = UserLevel.level(coin)

	val avatarPath: String get() = "${APIConfig.URL}/${ServerRes.Users.User(uid).avatar}"
}