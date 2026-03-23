package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File

@Stable
internal class WebAudioPlayer(context: PlatformContext, onEndListener: () -> Unit) : AudioPlayer(context, onEndListener) {
    override val isInit: Boolean = false
    override val isPlaying: Boolean = false
    override val position: Long = 0L
    override val duration: Long = 0L
    override suspend fun init() {}
    override suspend fun load(path: File, playing: Boolean) {}
    override fun play() {}
    override fun pause() {}
    override fun stop() {}
    override fun release() {}
    override fun seekTo(position: Long) {}
}

actual fun buildAudioPlayer(context: PlatformContext, onEndListener: () -> Unit): AudioPlayer = WebAudioPlayer(context, onEndListener)