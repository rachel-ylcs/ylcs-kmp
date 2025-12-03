package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class WeiboUserInfo(
	val id: String, // ID
	val name: String, // 昵称
	val avatar: String = "", // 头像
) {
	companion object {
		val DEFAULT = listOf(
			WeiboUserInfo("2266537042", "银临Rachel"),
			WeiboUserInfo("7802114712", "银临-欢银光临"),
			WeiboUserInfo("3965226022", "银临的小银库")
		)
	}
}