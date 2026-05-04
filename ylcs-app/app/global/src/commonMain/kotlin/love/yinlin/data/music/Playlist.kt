package love.yinlin.data.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
sealed interface Playlist {
    val name: String

    @Stable
    @Serializable
    data object None : Playlist {
        override val name: String = ""
    }

    @Stable
    @Serializable
    data object Default : Playlist {
        override val name: String = "默认歌单"
    }
    
    @Stable
    @Serializable
    data class User(override val name: String) : Playlist
}