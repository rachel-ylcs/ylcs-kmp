package love.yinlin.screen.world.single.rhyme

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.files.Path
import love.yinlin.Context
import love.yinlin.compose.game.Manager
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.platform.AudioPlayer
import love.yinlin.screen.world.single.rhyme.spirit.Scene

@Stable
class RhymeManager(
    context: Context,
    private val onComplete: () -> Unit
) : Manager() {
    override val size: Size = Size(1920f, 1080f)
    override val fps: Int = RhymeConfig.FPS
    override val currentTick: Long get() = mp.position

    override fun onSceneComplete() = onComplete()

    private val mp = AudioPlayer(context)

    suspend fun CoroutineScope.start(
        lyrics: RhymeLyricsConfig,
        record: ImageBitmap,
        audio: Path
    ) {
        mp.load(audio)
        onSceneCreate(Scene(
            manager = this@RhymeManager,
            lyrics = lyrics,
            record = record,
        ))
        resume()
    }

    fun CoroutineScope.resume() {
        mp.play()
        onSceneResume()
    }

    fun pause() {
        mp.pause()
        onScenePause()
    }

    fun stop() {
        mp.stop()
        onSceneStop()
    }

    suspend fun init() = mp.init()

    fun release() = mp.release()
}