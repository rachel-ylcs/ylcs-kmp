package love.yinlin.media.lyrics

import androidx.compose.runtime.Stable

@Stable
fun interface LyricsEngineHost {
    suspend fun seekTo(position: Long)
}