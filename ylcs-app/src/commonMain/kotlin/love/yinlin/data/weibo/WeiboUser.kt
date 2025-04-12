package love.yinlin.data.weibo

import androidx.compose.runtime.Stable

@Stable
data class WeiboUser(
	val info: WeiboUserInfo, // 用户信息
	val background: String, // 背景图
	val signature: String, // 个性签名
	val followNum: String, // 关注数
	val fansNum: String, // 粉丝数
)