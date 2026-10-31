package love.yinlin.media

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import love.yinlin.concurrent.atomic
import love.yinlin.coroutines.mainContext
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File

@Stable
internal class PcAudioPlayer(context: PlatformContext, onEndListener: () -> Unit) : AudioPlayer(context, onEndListener) {
    private val backend = AudioBackend()
    private val scope = CoroutineScope(SupervisorJob() + mainContext)
    private val desiredPlaying = atomic(false)

    init {
        scope.launch {
            for ((val revision, val kind) in backend.events) {
                yield()
                if (!backend.isClosed && revision == backend.revision && kind == AudioEvent.ENDED) onEndListener()
            }
        }
    }

    override val isInit: Boolean get() = backend.isInit
    override val isPlaying: Boolean get() = backend.query().isPlaying
    override val position: Long get() = backend.query().position
    override val duration: Long get() = backend.query().duration

    override suspend fun init() { backend.init() }

    override fun release() {
        backend.release()
        scope.cancel()
    }

    override suspend fun load(path: File, playing: Boolean) {
        if (!isInit) return

        val request = backend.nextRequest()
        desiredPlaying.value = playing
        check(backend.load(path.path, request)) { "Cannot load audio: ${backend.query().error}" }

        backend.checkCurrent(request)
        if (desiredPlaying.value) backend.play(request)
    }

    override fun play() {
        desiredPlaying.value = true
        backend.play()
    }

    override fun pause() {
        desiredPlaying.value = false
        backend.pause()
    }

    override fun stop() {
        desiredPlaying.value = false
        backend.stop()
    }

    override fun seekTo(position: Long) {
        backend.seek(position)
        play()
    }
}
