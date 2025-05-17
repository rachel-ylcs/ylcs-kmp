package love.yinlin.platform

import androidx.compose.runtime.Stable

@Stable
class ActualFloatingLyrics : FloatingLyrics() {
    override val canAttached: Boolean = false
    override val isAttached: Boolean = false

    override fun applyPermission(onResult: (Boolean) -> Unit) {

    }

    override fun attach() {

    }

    override fun detach() {

    }

    override fun updateLyrics(lyrics: String?) {

    }
}