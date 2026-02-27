package love.yinlin.common.rhyme

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