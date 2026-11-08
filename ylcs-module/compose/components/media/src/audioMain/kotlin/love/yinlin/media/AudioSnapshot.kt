package love.yinlin.media

import androidx.compose.runtime.Stable

@Stable
internal data class AudioSnapshot(val isPlaying: Boolean, val position: Long = 0L, val duration: Long = 0L, val error: Long = 0L) {
    companion object {
        val Empty = AudioSnapshot(false)
    }
}
