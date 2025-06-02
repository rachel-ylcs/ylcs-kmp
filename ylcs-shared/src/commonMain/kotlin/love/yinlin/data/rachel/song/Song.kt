package love.yinlin.data.rachel.song

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class Song(
    val sid: Int, // [歌曲编号]
    val version: Double, // [版本]
    val name: String, // [曲名]
    val singer: String, //[歌手名]
    val lyricist: String, // [词作]
    val composer: String, // [作曲人]
    val album: String, // [专辑]
    val bgd: Boolean, // [是否有伴奏]
    val video: Boolean, // [是否有MV]
)