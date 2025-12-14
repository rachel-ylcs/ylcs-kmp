package love.yinlin.platform.lyrics

import androidx.compose.runtime.Stable

@Stable
interface LyricsEngineHost {
    suspend fun seekTo(position: Long)
}