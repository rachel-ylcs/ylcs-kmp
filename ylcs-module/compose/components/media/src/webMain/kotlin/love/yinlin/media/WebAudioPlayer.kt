package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File

@Stable
internal class WebAudioPlayer(context: PlatformContext, onEndListener: () -> Unit) : AudioPlayer(context, onEndListener) {
    private val backend = WebAudioBackend(onEnded = { onEndListener() })

    override val isInit: Boolean get() = backend.isInit
    override val isPlaying: Boolean get() = backend.isPlaying
    override val position: Long get() = backend.position
    override val duration: Long get() = backend.duration

    override suspend fun init() = backend.init()

    override suspend fun load(path: File, playing: Boolean) {
        if (isInit) {
            backend.load(playing) { WebMediaSource.fromFile(path) }
        }
    }

    override fun play() = backend.play()
    override fun pause() = backend.pause()
    override fun stop() = backend.stop()
    override fun release() = backend.release()
    override fun seekTo(position: Long) = backend.seekTo(position)
}
