package love.yinlin.data.weibo

import androidx.compose.runtime.Stable

@Stable
data class WeiboUserInfo(
	val id: String, // ID
	val name: String, // 昵称
	val avatar: String, // 头像
)