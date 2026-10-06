package love.yinlin.media

import androidx.compose.runtime.Stable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File

@Stable
internal class IOSAudioPlayer(context: PlatformContext, onEndListener: () -> Unit) : AudioPlayer(context, onEndListener) {
    private val lifetime = IOSPlayerLifetime()
    private val scope = lifetime.scope
    private var player: IOSAVPlayer? = null

    override val isInit: Boolean get() = !lifetime.isClosed && player != null
    override val isPlaying: Boolean get() = isInit && player?.isPlaying == true
    override val position: Long get() = if (isInit) player?.position ?: 0L else 0L
    override val duration: Long get() = if (isInit) player?.duration ?: 0L else 0L

    override suspend fun init() = Coroutines.main {
        if (!lifetime.isClosed && player == null) {
            player = IOSAVPlayer(lifetime, onEnded = { generation ->
                val current = player
                if (!lifetime.isClosed && current?.generation == generation) {
                    onEndListener()
                }
            })
        }
    }

    override suspend fun load(path: File, playing: Boolean) = Coroutines.main {
        if (lifetime.isClosed) return@main
        val current = checkNotNull(player) { "AudioPlayer 尚未初始化" }
        if (!current.load(path.path, playing)) {
            throw current.lastError ?: IllegalStateException("音频加载失败")
        }
    }

    private fun command(block: IOSAVPlayer.() -> Unit) {
        scope.launch(Dispatchers.Main.immediate) {
            if (!lifetime.isClosed) player?.block()
        }
    }

    override fun play() = command { play() }
    override fun pause() = command { pause() }
    override fun stop() = command { stop() }
    override fun seekTo(position: Long) = command { seek(position) }

    override fun release() {
        lifetime.close {
            player?.release()
            player = null
        }
    }
}

actual fun buildAudioPlayer(context: PlatformContext, onEndListener: () -> Unit): AudioPlayer = IOSAudioPlayer(context, onEndListener)