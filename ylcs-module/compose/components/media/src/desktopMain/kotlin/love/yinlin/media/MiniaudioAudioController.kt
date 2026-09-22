package love.yinlin.media

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import love.yinlin.annotation.NativeLibApi
import love.yinlin.coroutines.mainContext
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File

@Stable
@NativeLibApi
internal open class MiniaudioAudioController(context: PlatformContext, onEndListener: () -> Unit) : AudioPlayer(context, onEndListener) {
    @Stable
    private enum class PlaybackState { None, Opening, Buffering, Playing, Paused; }

    private val scope = CoroutineScope(SupervisorJob() + mainContext)

    private var isRelease = false

    private var nativeHandle: Long = 0L

    override val isInit: Boolean get() = nativeHandle != 0L

    override val isPlaying: Boolean
        get() = nativeHandle != 0L && nativeGetPlaybackState(nativeHandle) == PlaybackState.Playing.ordinal

    override val position: Long get() = if (nativeHandle == 0L) 0L else nativeGetPosition(nativeHandle)

    override val duration: Long get() = if (nativeHandle == 0L) 0L else nativeGetDuration(nativeHandle)

    override suspend fun init() {
        if (!isRelease && nativeHandle == 0L) nativeHandle = nativeCreate()
    }

    override fun release() {
        if (!isRelease) {
            isRelease = true
            scope.cancel()
            if (nativeHandle != 0L) nativeRelease(nativeHandle)
            nativeHandle = 0L
        }
    }

    override suspend fun load(path: File, playing: Boolean) {
        if (nativeHandle != 0L && nativeSetSource(nativeHandle, path.path) && playing) nativePlay(nativeHandle)
    }

    override fun play() {
        if (nativeHandle != 0L && nativeGetPlaybackState(nativeHandle) != PlaybackState.Playing.ordinal) {
            nativePlay(nativeHandle)
        }
    }

    override fun pause() {
        if (nativeHandle != 0L && nativeGetPlaybackState(nativeHandle) == PlaybackState.Playing.ordinal) {
            nativePause(nativeHandle)
        }
    }

    override fun stop() {
        if (nativeHandle != 0L) nativeSetSource(nativeHandle, null)
    }

    override fun seekTo(position: Long) {
        if (nativeHandle != 0L) {
            nativeSeek(nativeHandle, position)
            if (nativeGetPlaybackState(nativeHandle) != PlaybackState.Playing.ordinal) nativePlay(nativeHandle)
        }
    }

    @Suppress("unused")
    private fun nativeMediaEnded() {
        scope.launch {
            if (!isRelease) onEndListener()
        }
    }

    private external fun nativeCreate(): Long
    private external fun nativeRelease(handle: Long)
    private external fun nativeGetPlaybackState(handle: Long): Int
    private external fun nativeGetPosition(handle: Long): Long
    private external fun nativeGetDuration(handle: Long): Long
    private external fun nativeSetSource(handle: Long, path: String?): Boolean
    private external fun nativePlay(handle: Long)
    private external fun nativePause(handle: Long)
    private external fun nativeSeek(handle: Long, position: Long)
}
