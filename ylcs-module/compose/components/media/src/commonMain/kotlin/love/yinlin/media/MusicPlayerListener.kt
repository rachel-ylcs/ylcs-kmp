package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.compose.data.media.MediaInfo
import love.yinlin.compose.data.media.MediaPlayMode

@Stable
interface MusicPlayerListener<Info : MediaInfo> {
    fun onMusicChanged(info: Info?)
    fun onPlayModeChanged(mode: MediaPlayMode)
    fun onPlayerStop()
}