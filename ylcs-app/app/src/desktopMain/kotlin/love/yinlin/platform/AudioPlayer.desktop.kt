package love.yinlin.platform

import androidx.compose.runtime.Stable
import kotlinx.io.files.Path
import love.yinlin.Context

@Stable
actual class AudioPlayer actual constructor(context: Context) {
    private var handle: Long = 0L

    actual val isInit: Boolean get() = handle != 0L

    actual val isPlaying: Boolean get() = nativeIsPlaying(handle)

    actual val position: Long get() = nativeGetPosition(handle)

    actual val duration: Long get() = nativeGetDuration(handle)

    actual suspend fun init() {
        handle = nativeCreatePlayer()
    }

    actual suspend fun load(path: Path) {
        Coroutines.io {
            nativeLoad(handle, path.toString())
            nativePlay(handle)
        }
    }

    actual fun play() {
        nativePlay(handle)
    }

    actual fun pause() {
        nativePause(handle)
    }

    actual fun stop() {
        nativeStop(handle)
    }

    actual fun release() {
        nativeReleasePlayer(handle)
        handle = 0L
    }
}