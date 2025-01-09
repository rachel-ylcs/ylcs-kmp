package love.yinlin.data.weibo

import androidx.compose.runtime.Stable

@Stable
data class WeiboUserInfo(
	val id: String, // ID
	val name: String, // 昵称
	val avatar: String, // 头像
) {
	companion object {
		val DEFAULT_WEIBO_USER_INFO get() = listOf(
			WeiboUserInfo("2266537042", "", ""),
			WeiboUserInfo("7802114712", "", ""),
			WeiboUserInfo("3965226022", "", "")
		)
	}
}