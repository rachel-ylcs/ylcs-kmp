package love.yinlin.compose.game.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.RhymeLyricsConfig

@Stable
class RhymePlayInfo(
    val playConfig: RhymePlayConfig,
    val musicInfo: MusicInfo,
    val lyricsConfig: RhymeLyricsConfig,
    val musicRecord: ImageBitmap
)