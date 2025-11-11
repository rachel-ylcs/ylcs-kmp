package love.yinlin.data.rachel.song

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class SongPreview(
    val sid: String,
    val version: String,
    val name: String,
)