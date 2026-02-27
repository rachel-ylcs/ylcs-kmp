package love.yinlin.media

import androidx.compose.runtime.Stable
import kotlinx.io.files.Path
import love.yinlin.annotation.NativeLibApi
import love.yinlin.foundation.Context

@Stable
@NativeLibApi
internal class MacOSAudioController(context: Context, onEndListener: () -> Unit) : AudioPlayer(context, onEndListener) {
    override val isInit: Boolean = false
    override val isPlaying: Boolean = false
    override val position: Long = 0L
    override val duration: Long = 0L
    override suspend fun init() {}
    override suspend fun load(path: Path) {}
    override fun play() {}
    override fun pause() {}
    override fun stop() {}
    override fun release() {}
    override fun seekTo(position: Long) {}
}