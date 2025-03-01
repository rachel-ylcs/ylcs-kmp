package love.yinlin.data.rachel

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.api.APIConfig
import love.yinlin.api.ServerRes

@Stable
@Serializable
data class UserProfile(
	val uid: Int, // [用户 ID]
	val name: String, // [昵称]
	// pwd: String [密码]
	val inviterName: String?, // [邀请人]
	val privilege: Int, // [权限]
	val signature: String, // [个性签名]
	val label: String, // [标签]
	val coin: Int, // [银币]
	// playlist: JsonObject [云歌单]
	// signin: Binary [签到记录]
) {
	@Stable
	val avatarPath: String get() = "${APIConfig.URL}/${ServerRes.Users.User(uid).avatar}"

	@Stable
	val wallPath: String get() = "${APIConfig.URL}/${ServerRes.Users.User(uid).wall}"

	@Stable
	val level: Int get() = UserLevel.level(coin)

	@Stable
	val hasPrivilegeBackup: Boolean get() = UserPrivilege.backup(privilege)
	@Stable
	val hasPrivilegeRes: Boolean get() = UserPrivilege.res(privilege)
	@Stable
	val hasPrivilegeTopic: Boolean get() = UserPrivilege.topic(privilege)
	@Stable
	val hasPrivilegeVIPAccount: Boolean get() = UserPrivilege.vipAccount(privilege)
	@Stable
	val hasPrivilegeVIPTopic: Boolean get() = UserPrivilege.vipTopic(privilege)
	@Stable
	val hasPrivilegeVIPCalendar: Boolean get() = UserPrivilege.vipCalendar(privilege)

	@Stable
	fun canUpdateTopicTop(topicUid: Int) = uid == topicUid
	@Stable
	fun canDeleteTopic(topicUid: Int) = uid == topicUid || hasPrivilegeVIPTopic
	@Stable
	fun canUpdateCommentTop(topicUid: Int) = uid == topicUid || hasPrivilegeVIPTopic
	@Stable
	fun canDeleteComment(topicUid: Int, commentUid: Int) = uid == topicUid || uid == commentUid || hasPrivilegeVIPTopic
}