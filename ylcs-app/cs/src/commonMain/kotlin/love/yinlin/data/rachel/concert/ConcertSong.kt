package love.yinlin.data.rachel.concert

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class ConcertSong(
    val sid: Int, // [演出歌曲 ID，与原有 Song.sid 独立]
    val name: String, // [歌名]
    val pid: String? = null, // [关联的原有 Song.sid，可空]
)
