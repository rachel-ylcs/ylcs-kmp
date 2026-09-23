package love.yinlin.media

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import love.yinlin.annotation.NativeLibApi
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.coroutines.mainContext
import love.yinlin.foundation.PlatformContext
import kotlin.time.Duration.Companion.milliseconds

@Stable
@NativeLibApi
internal open class MiniaudioMusicController(fetcher: MediaMetadataFetcher) : CommonMusicPlayer(fetcher) {
    @Stable
    private enum class PlaybackState { None, Opening, Buffering, Playing, Paused; }

    private val scope = CoroutineScope(SupervisorJob() + mainContext)

    private val isPlayingFlow = MutableStateFlow(false)

    private var isRelease = false

    private var nativeHandle: Long = 0L

    override suspend fun init(context: PlatformContext) {
        if (isRelease || nativeHandle != 0L) return

        nativeHandle = nativeCreate()
        if (nativeHandle == 0L) {
            if (error == null) error = IllegalStateException("Failed to initialize miniaudio")
            return
        }

        scope.launch {
            isPlayingFlow.collectLatest { value ->
                isPlaying = value
                if (value) {
                    while (this@launch.isActive && !isRelease) {
                        val pos = nativeGetPosition(nativeHandle)
                        position = pos
                        listener?.onPositionChanged(pos)
                        delay(fetcher.interval.milliseconds)
                    }
                }
            }
        }
        isInit = true
    }

    override fun release() {
        if (!isRelease) {
            isRelease = true
            isPlayingFlow.value = false
            scope.cancel()
            if (nativeHandle != 0L) nativeRelease(nativeHandle)
            nativeHandle = 0L
            isInit = false
        }
    }

    override suspend fun play() {
        if (isReady && nativeHandle != 0L && nativeGetPlaybackState(nativeHandle) != PlaybackState.Playing.ordinal) {
            nativePlay(nativeHandle)
        }
    }

    override suspend fun pause() {
        if (isReady && nativeHandle != 0L && nativeGetPlaybackState(nativeHandle) == PlaybackState.Playing.ordinal) {
            nativePause(nativeHandle)
        }
    }

    override suspend fun seekTo(position: Long) {
        if (isReady && nativeHandle != 0L) {
            nativeSeek(nativeHandle, position)
            if (nativeGetPlaybackState(nativeHandle) != PlaybackState.Playing.ordinal) nativePlay(nativeHandle)
        }
    }

    override fun innerStop() {
        if (nativeHandle != 0L) nativeSetSource(nativeHandle, null)
    }

    override fun innerGotoIndex(path: String, playing: Boolean): Boolean {
        if (nativeHandle == 0L || !nativeSetSource(nativeHandle, path)) return false
        if (playing) nativePlay(nativeHandle)
        return true
    }

    @Suppress("unused")
    private fun nativeDurationChange(duration: Long) {
        this.duration = duration.coerceAtLeast(0L)
    }

    @Suppress("unused")
    private fun nativePlaybackStateChange(value: Int) {
        when (value) {
            PlaybackState.Playing.ordinal -> isPlayingFlow.value = true
            PlaybackState.None.ordinal, PlaybackState.Paused.ordinal -> isPlayingFlow.value = false
        }
    }

    @Suppress("unused")
    private fun nativeSourceChange() {
        if (!isReady) return

        val newId = musicList.getOrNull(currentIndex)
        if (newId != currentId) {
            currentId = newId
            listener?.onMusicChanged(newId)
        }
    }

    @Suppress("unused")
    private fun nativeMediaEnded() {
        scope.launch {
            if (!isReady || isRelease) return@launch

            internalGotoIndex(when (playMode) {
                MediaPlayMode.Order -> loopNextIndex
                MediaPlayMode.Loop -> currentIndex
                MediaPlayMode.Random -> randomNextIndex ?: reshuffled()
            })
        }
    }

    @Suppress("unused")
    private fun nativeOnError(message: String) {
        error = IllegalStateException(message)
    }

    private external fun nativeCreate(): Long
    private external fun nativeRelease(handle: Long)
    private external fun nativeGetPlaybackState(handle: Long): Int
    private external fun nativeGetPosition(handle: Long): Long
    private external fun nativeSetSource(handle: Long, path: String?): Boolean
    private external fun nativePlay(handle: Long)
    private external fun nativePause(handle: Long)
    private external fun nativeSeek(handle: Long, position: Long)
}
