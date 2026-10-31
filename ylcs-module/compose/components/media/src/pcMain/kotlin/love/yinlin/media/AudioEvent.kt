package love.yinlin.media

import androidx.compose.runtime.Stable

@Stable
internal data class AudioEvent(val revision: Long, val kind: Int, val value: Long, val message: String?) {
    companion object {
        const val PLAYBACK_STATE = 0
        const val DURATION = 1
        const val SOURCE = 2
        const val ENDED = 3
        const val ERROR = 4
        const val FRAME = 5
    }
}
