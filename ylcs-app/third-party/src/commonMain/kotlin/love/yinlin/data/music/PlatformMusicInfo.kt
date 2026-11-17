package love.yinlin.data.music

import androidx.compose.runtime.Stable

@Stable
data class PlatformMusicInfo(
    val id: String,
    val name: String,
    val singer: String,
    val pic: String,
    internal val miguSource: MiguSource? = null,
) {
    @Stable
    data class MiguSource(val copyrightId: String, val lyricUrl: String?)
}
