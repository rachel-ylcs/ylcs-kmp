package love.yinlin.compose.data.media

import androidx.compose.runtime.Stable

@Stable
interface MediaInfo {
    val id: String
    val name: String
    val singer: String
    val lyricist: String
    val composer: String
    val album: String
}