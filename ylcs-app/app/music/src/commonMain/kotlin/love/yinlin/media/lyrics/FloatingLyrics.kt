package love.yinlin.media.lyrics

import androidx.compose.runtime.Stable
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.startup.StartupMusicPlayer

@Stable
expect class FloatingLyrics(startup: StartupMusicPlayer) {
    var isAttached: Boolean
        private set

    fun attach()

    fun detach()

    suspend fun initDelay()
}