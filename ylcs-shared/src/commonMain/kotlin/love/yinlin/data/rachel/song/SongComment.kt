package love.yinlin.data.rachel.song

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable


@Stable
@Serializable
data class SongComment(
    val cid:Int,//[评论编号]
    val sid: Int, // [歌曲编号]
    val uid: Int, // [评论者ID]
    val ts: String, // [评论时间]
    val content:String,//[评论内容]
    val name:String,//[评论者名称]
)