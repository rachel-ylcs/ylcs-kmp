package love.yinlin.media

import androidx.compose.runtime.Stable
import kotlinx.io.files.Path
import love.yinlin.annotation.NativeLibApi
import love.yinlin.foundation.Context

@Stable
@NativeLibApi
internal class WindowsAudioController(context: Context, onEndListener: () -> Unit) : AudioPlayer(context, onEndListener) {
    @Stable
    private enum class PlaybackState { None, Opening, Buffering, Playing, Paused; }

    private var isRelease = false

    private var nativeHandle: Long = 0L

    override val isInit: Boolean get() = nativeHandle != 0L

    override val isPlaying: Boolean get() = nativeGetPlaybackState(nativeHandle) == PlaybackState.Playing.ordinal

    override val position: Long get() = nativeGetPosition(nativeHandle)

    override val duration: Long get() = nativeGetDuration(nativeHandle)

    override suspend fun init() {
        nativeHandle = nativeCreate()
    }

    override fun release() {
        if (!isRelease) {
            isRelease = true
            nativeRelease(nativeHandle)
            nativeHandle = 0L
        }
    }

    override suspend fun load(path: Path) {
        nativeSetSource(nativeHandle, path.toString())
        nativePlay(nativeHandle)
    }

    override fun play() {
        if (nativeGetPlaybackState(nativeHandle) != PlaybackState.Playing.ordinal) nativePlay(nativeHandle)
    }

    override fun pause() {
        if (nativeGetPlaybackState(nativeHandle) == PlaybackState.Playing.ordinal) nativePause(nativeHandle)
    }

    override fun stop() {
        nativeSetSource(nativeHandle, null)
    }

    override fun seekTo(position: Long) {
        nativeSeek(nativeHandle, position)
        if (nativeGetPlaybackState(nativeHandle) != PlaybackState.Playing.ordinal) nativePlay(nativeHandle)
    }

    @Suppress("unused")
    private fun nativeMediaEnded() = onEndListener()

    private external fun nativeCreate(): Long
    private external fun nativeRelease(handle: Long)
    private external fun nativeGetPlaybackState(handle: Long): Int
    private external fun nativeGetPosition(handle: Long): Long
    private external fun nativeGetDuration(handle: Long): Long
    private external fun nativeSetSource(handle: Long, path: String?)
    private external fun nativePlay(handle: Long)
    private external fun nativePause(handle: Long)
    private external fun nativeSeek(handle: Long, position: Long)
}