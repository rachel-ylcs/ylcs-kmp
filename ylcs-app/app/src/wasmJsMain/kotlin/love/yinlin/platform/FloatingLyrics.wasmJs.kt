package love.yinlin.platform

import androidx.compose.runtime.Stable

@Stable
class ActualFloatingLyrics : FloatingLyrics() {
    override val isAttached: Boolean = false

    override fun updateLyrics(lyrics: String?) {

    }
}