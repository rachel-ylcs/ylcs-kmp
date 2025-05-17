package love.yinlin.platform

import androidx.compose.runtime.*

@Stable
abstract class FloatingLyrics {
    protected var currentLyrics: String? by mutableStateOf(null)

    abstract val canAttached: Boolean
    abstract val isAttached: Boolean
    abstract fun applyPermission(onResult: (Boolean) -> Unit)
    abstract fun attach()
    abstract fun detach()
    abstract fun updateLyrics(lyrics: String?)
}