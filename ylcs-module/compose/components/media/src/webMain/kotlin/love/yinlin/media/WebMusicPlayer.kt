package love.yinlin.media

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.coroutines.mainContext
import love.yinlin.extension.catchingError
import love.yinlin.foundation.PlatformContext
import kotlin.time.Duration.Companion.milliseconds

@Stable
class WebMusicPlayer(fetcher: MediaMetadataFetcher) : CommonMusicPlayer(fetcher) {
    private val scope = CoroutineScope(SupervisorJob() + mainContext)
    private var positionJob: Job? = null

    private val backend = WebAudioBackend(
        onStateChanged = ::updatePlayback,
        onError = { error = it },
        onEnded = ::advance,
    )

    override suspend fun init(context: PlatformContext) {
        backend.init()
        isInit = backend.isInit
    }

    override fun release() {
        listener = null
        scope.cancel()
        backend.release()
        isInit = false
    }

    override suspend fun play() {
        if (isReady && isInit) {
            error = null
            backend.play()
        }
    }

    override suspend fun pause() { backend.pause() }

    override suspend fun seekTo(position: Long) {
        if (isReady && isInit) backend.seekTo(position)
    }

    override suspend fun innerStop() { backend.stop() }

    override suspend fun innerGotoIndex(path: String, playing: Boolean): Boolean {
        if (!isInit) return false

        error = null

        val failure = catchingError { backend.load(playing) { WebMediaSource.resolve(path) } }
        if (failure != null) {
            error = failure
            return false
        }

        val id = musicList.getOrNull(currentIndex)
        if (id != currentId) {
            currentId = id
            listener?.onMusicChanged(id)
        }

        return true
    }

    private fun updatePosition() {
        position = backend.position
        listener?.onPositionChanged(position)
    }

    private fun updatePlayback() {
        val playing = backend.isPlaying
        isPlaying = playing
        duration = backend.duration
        updatePosition()

        if (playing) {
            error = null

            if (positionJob?.isActive != true) {
                positionJob = scope.launch {
                    while (isActive && backend.isPlaying) {
                        delay(fetcher.interval.coerceAtLeast(1L).milliseconds)
                        updatePosition()
                    }
                }
            }
        }
        else {
            positionJob?.cancel()
            positionJob = null
        }
    }

    private fun advance(revision: Long) {
        scope.launch {
            if (revision != backend.revision || !isReady || !isInit) return@launch

            internalGotoIndex(when (playMode) {
                MediaPlayMode.Order -> loopNextIndex
                MediaPlayMode.Loop -> currentIndex
                MediaPlayMode.Random -> randomNextIndex ?: reshuffled()
            })
        }
    }
}
