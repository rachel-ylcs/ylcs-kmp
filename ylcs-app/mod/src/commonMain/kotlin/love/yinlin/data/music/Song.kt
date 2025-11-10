package love.yinlin.data.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class Song(
    val id: String,
    val version: String,
    val name: String,
    val singer: String,
    val lyricist: String,
    val composer: String,
    val album: String,
    val animation: Boolean = false,
    val video: Boolean = false,
    val rhyme: Boolean = false,
)