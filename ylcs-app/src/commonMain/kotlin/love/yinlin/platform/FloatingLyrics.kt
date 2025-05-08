package love.yinlin.platform

import androidx.compose.runtime.*

@Stable
abstract class FloatingLyrics {
    protected var currentLyrics: String by mutableStateOf("")

    abstract val isAttached: Boolean
    abstract fun attach()
    abstract fun detach()
    abstract fun updateLyrics(lyrics: String)
}