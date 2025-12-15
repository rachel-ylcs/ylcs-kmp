package love.yinlin.screen.world.single.rhyme

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.music.MusicInfo

@Serializable
@Stable
data class RhymeResult(
    val playConfig: RhymePlayConfig,
    val musicInfo: MusicInfo,
    val playResult: RhymePlayResult
)