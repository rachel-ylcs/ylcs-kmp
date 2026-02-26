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
import love.yinlin.compose.data.media.MediaInfo
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.coroutines.mainContext
import love.yinlin.foundation.Context

@Stable
@NativeLibApi
internal class WindowsMusicController<Info : MediaInfo>(fetcher: MediaMetadataFetcher<Info>) : CommonMusicPlayer<Info>(fetcher) {
    @Stable
    private enum class PlaybackState { None, Opening, Buffering, Playing, Paused; }

    private val scope = CoroutineScope(SupervisorJob() + mainContext)

    private var isRelease = false

    private var nativeHandle: Long = 0L

    private val isPlayingFlow = MutableStateFlow(false)

    private var shouldImmediatePlay: Boolean = false

    override suspend fun init(context: Context) {
        nativeHandle = nativeCreate()
        scope.launch {
            isPlayingFlow.collectLatest { value ->
                isPlaying = value
                if (value) {
                    while (isActive && !isRelease) {
                        position = nativeGetPosition(nativeHandle)
                        delay(fetcher.interval)
                    }
                }
            }
        }
        isInit = nativeHandle != 0L
    }

    override fun release() {
        if (!isRelease) {
            scope.cancel()
            isRelease = true
            nativeRelease(nativeHandle)
            nativeHandle = 0L
        }
    }

    override suspend fun play() {
        if (isReady && nativeGetPlaybackState(nativeHandle) != PlaybackState.Playing.ordinal) nativePlay(nativeHandle)
    }

    override suspend fun pause() {
        if (isReady && nativeGetPlaybackState(nativeHandle) == PlaybackState.Playing.ordinal) nativePause(nativeHandle)
    }

    override suspend fun seekTo(position: Long) {
        if (isReady) {
            nativeSeek(nativeHandle, position)
            if (nativeGetPlaybackState(nativeHandle) != PlaybackState.Playing.ordinal) nativePlay(nativeHandle)
        }
    }

    override fun innerStop() {
        musicList.clear()
        music = null
        duration = 0L
        currentIndex = -1
        resetShuffled()
        nativeSetSource(nativeHandle, null)
        listener?.onPlayerStop()
    }

    override fun innerGotoIndex(index: Int, playing: Boolean) {
        if (index in musicList.indices) {
            currentIndex = index
            val uri = with(fetcher) { musicList[index].audioUri }
            nativeSetSource(nativeHandle, uri)
            shouldImmediatePlay = playing
        }
        else innerStop()
    }

    // Callback
    @Suppress("unused")
    private fun nativeDurationChange(duration: Long) {
        this.duration = duration
    }

    @Suppress("unused")
    private fun nativePlaybackStateChange(value: Int) {
        when (value) {
            PlaybackState.Playing.ordinal -> isPlayingFlow.value = true
            PlaybackState.None.ordinal, PlaybackState.Paused.ordinal -> isPlayingFlow.value = false
            PlaybackState.Opening.ordinal -> if (shouldImmediatePlay) nativePlay(nativeHandle)
        }
    }

    @Suppress("unused")
    private fun nativeSourceChange() {
        if (!isReady) return

        val newInfo = musicList.getOrNull(currentIndex)
        if (newInfo?.id != music?.id) {
            music = newInfo
            listener?.onMusicChanged(newInfo)
        }
    }

    @Suppress("unused")
    private fun nativeMediaEnded() {
        if (!isReady) return

        innerGotoIndex(when (playMode) {
            MediaPlayMode.Order -> loopNextIndex
            MediaPlayMode.Loop -> currentIndex
            MediaPlayMode.Random -> randomNextIndex ?: reshuffled()
        })
    }

    @Suppress("unused")
    private fun nativeOnError(message: String) {
        error = IllegalStateException(message)
    }

    private external fun nativeCreate(): Long
    private external fun nativeRelease(handle: Long)
    private external fun nativeGetPlaybackState(handle: Long): Int
    private external fun nativeGetPosition(handle: Long): Long
    private external fun nativeSetSource(handle: Long, path: String?)
    private external fun nativePlay(handle: Long)
    private external fun nativePause(handle: Long)
    private external fun nativeSeek(handle: Long, position: Long)
}