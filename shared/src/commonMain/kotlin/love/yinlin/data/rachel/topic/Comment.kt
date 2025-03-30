package love.yinlin.data.rachel.topic

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.Local
import love.yinlin.api.ServerRes
import love.yinlin.data.rachel.profile.UserLevel
import love.yinlin.extension.DateEx

@Stable
@Serializable
data class Comment(
	val cid: Int, // [评论 ID]
	// val pid: Int? [父评论 ID]
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
) : Comparable<Comment> {
	object Section {
		const val LATEST = -1
		const val HOT = -2

		const val UNGROUPED = 0
		const val NOTIFICATION = 1
		const val WATER = 2
		const val ACTIVITY = 3
		const val DISCUSSION = 4

		val MovableSection = listOf(
			NOTIFICATION, WATER, ACTIVITY, DISCUSSION
		)

		fun sectionName(section: Int): String = when (section) {
			LATEST -> "最新"
			HOT -> "热门"
			NOTIFICATION -> "公告"
			WATER -> "水贴"
			ACTIVITY -> "活动"
			DISCUSSION -> "交流"
			else -> "未分组"
		}

		fun commentTable(section: Int): String = when (section) {
			NOTIFICATION -> "comment_notification"
			WATER -> "comment_water"
			ACTIVITY -> "comment_activity"
			DISCUSSION -> "comment_discussion"
			else -> ""
		}
	}

	override fun compareTo(other: Comment): Int {
		val v1 = other.isTop.compareTo(this.isTop)
		if (v1 != 0) return v1
		val time1 = DateEx.Formatter.standardDateTime.parse(this.ts)
		val time2 = DateEx.Formatter.standardDateTime.parse(other.ts)
		if (time1 == null) return -1
		if (time2 == null) return 1
		return time1.compareTo(time2)
	}

	val level: Int by lazy { UserLevel.level(coin) }

	val avatarPath: String by lazy { "${Local.ClientUrl}/${ServerRes.Users.User(uid).avatar}" }
}