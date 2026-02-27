package love.yinlin.common.rhyme

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.files.Path
import love.yinlin.compose.game.Manager
import love.yinlin.compose.game.asset.ImageAsset
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.foundation.Context
import love.yinlin.common.rhyme.spirit.Scene
import love.yinlin.media.SoundPlayer
import love.yinlin.media.buildAudioPlayer

@Stable
class RhymeManager(
    context: Context,
    onComplete: (RhymePlayResult) -> Unit,
    val onPause: () -> Unit
) : Manager() {
    override val size: Size = Size(RhymeConfig.WIDTH, RhymeConfig.HEIGHT)
    override val fps: Int = RhymeConfig.FPS
    override val currentTick: Long get() = ap.position

    override val assets: RhymeAssets = RhymeAssets()

    private val ap = buildAudioPlayer(context) {
        (scene as? Scene)?.playResult?.let(onComplete)
        stop()
    }

    private val sp = SoundPlayer()

    var config = RhymePlayConfig.Default

    val isInit: Boolean get() = ap.isInit
    val duration: Long get() = ap.duration

    suspend fun init() = ap.init()

    fun release() {
        ap.release()
        sp.release()
    }

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

        ap.load(audio)

        resume()
    }

    fun CoroutineScope.resume() {
        ap.play()
        onSceneResume()
    }

    fun pause() {
        ap.pause()
        onScenePause()
    }

    fun stop() {
        ap.stop()
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