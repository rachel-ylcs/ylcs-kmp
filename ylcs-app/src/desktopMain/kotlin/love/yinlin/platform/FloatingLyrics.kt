package love.yinlin.platform

import androidx.compose.runtime.*

@Stable
class ActualFloatingLyrics : FloatingLyrics() {
    override val canAttached: Boolean = true
    override var isAttached: Boolean by mutableStateOf(false)

    override fun applyPermission(onResult: (Boolean) -> Unit) {}

    override fun attach() {
        if (!isAttached) isAttached = true
    }

    override fun detach() {
        if (isAttached) isAttached = false
    }

    override fun updateLyrics(lyrics: String?) {

    }
}