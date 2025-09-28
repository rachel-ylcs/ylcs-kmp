package love.yinlin.data.rachel.song

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.Local
import love.yinlin.api.ServerRes
import love.yinlin.data.rachel.profile.UserLevel

@Stable
@Serializable
data class SongComment(
    val cid: Long, // [评论编号]
    val uid: Int, // [评论者ID]
    val ts: String, // [评论时间]
    val content: String, // [评论内容]
    val name: String, // [用户昵称]
    val label: String, // [用户标签]
    val exp: Int // [用户经验]
) {
    val level: Int by lazy { UserLevel.level(exp) }

    val avatarPath: String by lazy { "${Local.API_BASE_URL}/${ServerRes.Users.User(uid).avatar}" }
}