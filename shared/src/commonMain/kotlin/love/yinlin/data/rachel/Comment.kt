package love.yinlin.data.rachel

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.api.APIConfig
import love.yinlin.api.ServerRes

@Stable
@Serializable
data class Comment(
	val cid: Int, // [评论 ID]
	val pid: Int, // [父评论 ID]
	// tid: Int [主题 ID]
	val uid: Int, // [用户 ID]
	val ts: String, // [发布时间]
	val content: String, // [内容]
	val isTop: Boolean, // [是否置顶]
	// isDeleted: Boolean [是否删除]
	val subCommentNum: Int, // [楼中楼数量]
	val name: String, // [用户昵称]
	val label: String, // [用户标签]
	val coin: Int // [用户银币]
) {
	object Section {
		const val LATEST = -1 // 最新
		const val HOT = -2 // 热门

		const val UNGROUPED = 0 // 未分组
		const val NOTIFICATION = 1 // 通知
		const val WATER = 2 // 水贴
		const val ACTIVITY = 3 // 活动
		const val DISCUSSION = 4 // 讨论

		fun commentTable(section: Int): String = when (section) {
			NOTIFICATION -> "comment_notification"
			WATER -> "comment_water"
			ACTIVITY -> "comment_activity"
			DISCUSSION -> "comment_discussion"
			else -> ""
		}
	}

	@Stable
	val level: Int get() = UserLevel.level(coin)

	@Stable
	val avatarPath: String get() = "${APIConfig.URL}/${ServerRes.Users.User(uid).avatar}"
}