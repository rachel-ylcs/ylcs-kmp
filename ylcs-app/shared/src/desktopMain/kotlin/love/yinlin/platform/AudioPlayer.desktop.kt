package love.yinlin.platform

import androidx.compose.runtime.Stable
import kotlinx.io.files.Path
import love.yinlin.annotation.NativeLibApi
import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.Context

@Stable
@NativeLibApi
actual class AudioPlayer actual constructor(context: Context, private val onEndListener: () -> Unit) {
    private val nativeAudioPlayer = WindowsNativeAudioPlayer()

    actual val isInit: Boolean get() = nativeAudioPlayer.isInit

    actual val isPlaying: Boolean get() = nativeAudioPlayer.playbackState == WindowsNativePlaybackState.Playing

    actual val position: Long get() = nativeAudioPlayer.position

    actual val duration: Long get() = nativeAudioPlayer.duration

    actual suspend fun init() {
        nativeAudioPlayer.create(object : WindowsNativeAudioPlayer.Listener() {
            override fun onMediaEnded() {
                onEndListener()
            }
        })
    }

    actual suspend fun load(path: Path) {
        Coroutines.io {
            nativeAudioPlayer.load(path.toString())
            nativeAudioPlayer.play()
        }
    }

    actual fun play() {
        nativeAudioPlayer.play()
    }

    actual fun pause() {
        nativeAudioPlayer.pause()
    }

    actual fun stop() {
        nativeAudioPlayer.stop()
    }

    actual fun release() {
        nativeAudioPlayer.release()
    }
}