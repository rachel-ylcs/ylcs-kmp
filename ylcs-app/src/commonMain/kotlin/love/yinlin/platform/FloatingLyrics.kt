package love.yinlin.platform

import androidx.compose.runtime.*

@Stable
abstract class FloatingLyrics {
    protected var currentLyrics: String by mutableStateOf("")

    abstract fun updateLyrics(lyrics: String)
}