package love.yinlin.platform

import androidx.compose.runtime.Stable
import kotlinx.io.files.Path
import love.yinlin.Context

@Stable
expect class AudioPlayer(context: Context) {
    val isInit: Boolean
    val isPlaying: Boolean
    val position: Long
    val duration: Long
    suspend fun init()
    suspend fun load(path: Path)
    fun play()
    fun pause()
    fun stop()
    fun release()
}