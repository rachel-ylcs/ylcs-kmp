package love.yinlin.data.rachel

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.api.APIConfig
import love.yinlin.api.ServerRes

@Stable
@Serializable
data class Topic(
	val tid: Int, // [主题 ID]
	val uid: Int, // [用户 ID]
	// ts: String [发布时间]
	val title: String, // [标题]
	// content: String [内容]
	val pic: String?, // [图片集]
	val isTop: Boolean, // [是否置顶]
	val coinNum: Int, // [主题银币数]
	val commentNum: Int, // [主题评论数]
	// isDeleted: Boolean [是否删除]
	// section: Int [所属板块]
	val rawSection: Int, // [原始板块]
	val name: String, // [用户昵称]
) {
	@Stable
	val picPath: String get() = pic?.let { "${APIConfig.URL}/${ServerRes.Users.User(uid).Pics().pic(it)}" } ?: ""

	@Stable
	val avatarPath: String get() = "${APIConfig.URL}/${ServerRes.Users.User(uid).avatar}"
}