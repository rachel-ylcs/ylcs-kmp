package love.yinlin.screen.world.single.rhyme

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.files.Path
import love.yinlin.Context
import love.yinlin.compose.game.Manager
import love.yinlin.compose.game.asset.ImageAsset
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.platform.AudioPlayer
import love.yinlin.platform.SoundPlayer
import love.yinlin.screen.world.single.rhyme.spirit.Scene

@Stable
class RhymeManager(
    context: Context,
    onComplete: () -> Unit,
    val onPause: () -> Unit
) : Manager() {
    override val size: Size = Size(RhymeConfig.WIDTH, RhymeConfig.HEIGHT)
    override val fps: Int = RhymeConfig.FPS
    override val currentTick: Long get() = mp.position

    override val assets: RhymeAssets = RhymeAssets()

    private val mp = AudioPlayer(context) {
        stop()
        onComplete()
    }

    private val sp = SoundPlayer()

    var config = RhymePlayConfig.Default

    val isInit: Boolean get() = mp.isInit
    val duration: Long get() = mp.duration

    suspend fun init() = mp.init()

    fun release() = mp.release()

    suspend fun CoroutineScope.start(
        playConfig: RhymePlayConfig,
        name: String,
        lyricsConfig: RhymeLyricsConfig,
        recordImage: ByteArray,
        audio: Path
    ) {
        config = playConfig

        assets["mainRecord"] = ImageAsset.buildImmediately(recordImage)

        onSceneCreate(Scene(
            rhymeManager = this@RhymeManager,
            playConfig = playConfig,
            name = name,
            lyricsConfig = lyricsConfig
        ))

        mp.load(audio)

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

    fun playSound(sound: RhymeSound) {
        sp.play(sound.index)
    }

    suspend fun CoroutineScope.downloadAssets() {
        assets.apply {
            require(init())
            sp.loadFromPath(initSound())
        }
    }
}