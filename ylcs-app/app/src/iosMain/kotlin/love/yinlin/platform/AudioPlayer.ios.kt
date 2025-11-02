package love.yinlin.platform

import androidx.compose.runtime.Stable
import kotlinx.io.files.Path
import love.yinlin.Context

@Stable
actual class MusicPlayer actual constructor(context: Context) {
    actual val isInit: Boolean = false
    actual val isPlaying: Boolean = false
    actual val position: Long = 0L
    actual val duration: Long = 0L
    actual suspend fun init() {}
    actual suspend fun load(path: Path) {}
    actual fun play() {}
    actual fun pause() {}
    actual fun stop() {}
    actual fun release() {}
}