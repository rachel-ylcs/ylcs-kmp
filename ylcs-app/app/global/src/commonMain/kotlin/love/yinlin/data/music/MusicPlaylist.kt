package love.yinlin.data.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class MusicPlaylist(
    val name: String,
    val items: List<String>
) {
    constructor(name: String, item: String) : this(name, listOf(item))
}