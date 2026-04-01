package love.yinlin.media.lyrics

import androidx.compose.runtime.Stable
import love.yinlin.startup.StartupMusicPlayer

@Stable
actual class FloatingLyrics actual constructor(val mp: StartupMusicPlayer) {
    actual var isAttached: Boolean = false

    actual fun attach() { }

    actual fun detach() { }

    actual suspend fun initDelay() { }
}