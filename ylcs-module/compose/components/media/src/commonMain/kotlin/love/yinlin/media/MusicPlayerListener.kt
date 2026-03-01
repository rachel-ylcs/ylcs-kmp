package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.compose.data.media.MediaPlayMode

@Stable
interface MusicPlayerListener {
    fun onMusicChanged(id: String?)
    fun onPlayModeChanged(mode: MediaPlayMode)
    fun onPlayerStop()
}