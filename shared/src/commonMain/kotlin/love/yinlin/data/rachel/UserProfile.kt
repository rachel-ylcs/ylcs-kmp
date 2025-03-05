package love.yinlin.data.rachel

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.Local
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
	val avatarPath: String by lazy { "${Local.ClientUrl}/${ServerRes.Users.User(uid).avatar}" }

	val wallPath: String by lazy { "${Local.ClientUrl}/${ServerRes.Users.User(uid).wall}" }

	val level: Int by lazy { UserLevel.level(coin) }

	val hasPrivilegeBackup: Boolean get() = UserPrivilege.backup(privilege)

	val hasPrivilegeRes: Boolean get() = UserPrivilege.res(privilege)

	val hasPrivilegeTopic: Boolean get() = UserPrivilege.topic(privilege)

	val hasPrivilegeVIPAccount: Boolean get() = UserPrivilege.vipAccount(privilege)

	val hasPrivilegeVIPTopic: Boolean get() = UserPrivilege.vipTopic(privilege)

	val hasPrivilegeVIPCalendar: Boolean get() = UserPrivilege.vipCalendar(privilege)

	fun canUpdateTopicTop(topicUid: Int) = uid == topicUid

	fun canDeleteTopic(topicUid: Int) = uid == topicUid || hasPrivilegeVIPTopic

	fun canUpdateCommentTop(topicUid: Int) = uid == topicUid || hasPrivilegeVIPTopic

	fun canDeleteComment(topicUid: Int, commentUid: Int) = uid == topicUid || uid == commentUid || hasPrivilegeVIPTopic
}