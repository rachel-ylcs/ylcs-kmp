package love.yinlin.data.music

import androidx.compose.runtime.Stable

@Stable
internal data class KugouMusicInfo(
    override val id: String,
    override val name: String,
    override val singer: String,
    override val pic: String,
) : PlatformMusicInfo
