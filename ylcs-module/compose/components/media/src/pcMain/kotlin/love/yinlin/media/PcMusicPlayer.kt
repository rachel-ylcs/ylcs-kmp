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
import kotlinx.coroutines.yield
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.mainContext
import love.yinlin.extension.catchingError
import love.yinlin.foundation.PlatformContext
import kotlin.time.Duration.Companion.milliseconds

@Stable
internal abstract class PcMusicPlayer(fetcher: MediaMetadataFetcher) : CommonMusicPlayer(fetcher) {
    protected val backend = AudioBackend(music = true)
    protected var shouldImmediatePlay = false
    private val scope = CoroutineScope(SupervisorJob() + mainContext)
    private val playback = MutableStateFlow(false)
    private var observing = false

    protected abstract fun nativePlaybackStateChange(value: Int)

    protected fun updatePlayback(playing: Boolean) {
        isPlaying = playing
        playback.value = playing
    }

    override suspend fun init(context: PlatformContext) = Coroutines.mainImmediate {
        if (!observing && !backend.isClosed) {
            observing = true

            scope.launch {
                for (event in backend.events) {
                    yield()
                    if (!backend.isClosed && event.revision == backend.revision) onNativeEvent(event)
                }
            }

            scope.launch {
                playback.collectLatest { playing ->
                    if (playing) {
                        while (this@launch.isActive && isInit && !backend.isClosed) {
                            val current = backend.query().position
                            position = current
                            listener?.onPositionChanged(current)
                            delay(fetcher.interval.milliseconds)
                        }
                    }
                }
            }
        }

        error = catchingError { backend.init() }
        isInit = backend.isInit
    }

    override fun release() {
        backend.release()
        scope.cancel()
        listener = null
        isInit = false
        isPlaying = false
    }

    override suspend fun play() = Coroutines.mainImmediate {
        if (isReady && isInit) {
            shouldImmediatePlay = true
            error = catchingError { backend.play() }
        }
    }

    override suspend fun pause() = Coroutines.mainImmediate {
        shouldImmediatePlay = false
        backend.pause()
        updatePlayback(false)
    }

    override suspend fun seekTo(position: Long) = Coroutines.mainImmediate {
        if (isReady && isInit) {
            shouldImmediatePlay = true
            backend.seek(position)
            backend.play()
            val current = backend.query().position
            this@PcMusicPlayer.position = current
            listener?.onPositionChanged(current)
        }
    }

    override suspend fun innerStop() = Coroutines.mainImmediate {
        shouldImmediatePlay = false
        updatePlayback(false)
        position = 0L
        duration = 0L
        backend.stopAndWait()
    }

    override suspend fun innerGotoIndex(path: String, playing: Boolean): Boolean = Coroutines.mainImmediate {
        if (!isInit) return@mainImmediate false
        val request = backend.nextRequest()
        shouldImmediatePlay = playing
        updatePlayback(false)
        position = 0L
        duration = 0L
        error = null

        val failure = catchingError { check(backend.load(path, request)) { "Cannot load music: ${backend.query().error}" } }
        backend.checkCurrent(request)
        if (failure != null) {
            error = failure
            return@mainImmediate false
        }

        nativeSourceChange()
        if (shouldImmediatePlay) backend.play(request)
        true
    }

    private fun nativeSourceChange() {
        val id = musicList.getOrNull(currentIndex)
        if (id != currentId) {
            currentId = id
            listener?.onMusicChanged(id)
        }
    }

    private suspend fun onNativeEvent(event: AudioEvent) {
        when (event.kind) {
            AudioEvent.PLAYBACK_STATE -> nativePlaybackStateChange(event.value.toInt())
            AudioEvent.DURATION -> duration = event.value.coerceAtLeast(0L)
            AudioEvent.SOURCE -> nativeSourceChange()
            AudioEvent.ERROR -> {
                updatePlayback(false)
                error = IllegalStateException(event.message ?: "Audio playback failed: ${event.value}")
            }
            AudioEvent.ENDED -> {
                updatePlayback(false)
                position = duration
                listener?.onPositionChanged(position)
                if (isReady && isInit) {
                    internalGotoIndex(when (playMode) {
                        MediaPlayMode.Order -> loopNextIndex
                        MediaPlayMode.Loop -> currentIndex
                        MediaPlayMode.Random -> randomNextIndex ?: reshuffled()
                    })
                }
            }
        }
    }
}
