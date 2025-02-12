package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class WeiboUserLocal(
	val id: String, // ID
	val name: String, // 昵称
) {
	companion object {
		val DEFAULT get() = listOf(
			WeiboUserLocal("2266537042", "银临Rachel"),
			WeiboUserLocal("7802114712", "银临-欢银光临"),
			WeiboUserLocal("3965226022", "银临的小银库")
		)
	}
}