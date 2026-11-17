package love.yinlin.data.music

import androidx.compose.runtime.Stable

@Stable
interface PlatformMusicInfo {
    val id: String
    val name: String
    val singer: String
    val pic: String
}
