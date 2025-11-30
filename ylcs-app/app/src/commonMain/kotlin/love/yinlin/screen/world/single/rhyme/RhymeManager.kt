package love.yinlin.screen.world.single.rhyme

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.io.files.Path
import love.yinlin.Context
import love.yinlin.api.ServerRes
import love.yinlin.api.url
import love.yinlin.common.downloadCache
import love.yinlin.compose.game.Asset
import love.yinlin.compose.game.Manager
import love.yinlin.compose.graphics.decode
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.platform.AudioPlayer
import love.yinlin.platform.NetClient
import love.yinlin.resources.Res
import love.yinlin.resources.test
import love.yinlin.screen.world.single.rhyme.spirit.Scene
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment

@Stable
class RhymeManager(
    context: Context,
    onComplete: () -> Unit,
    val onPause: () -> Unit
) : Manager() {
    override val size: Size = Size(1920f, 1080f)
    override val fps: Int = RhymeConfig.FPS
    override val currentTick: Long get() = mp.position

    private val mp = AudioPlayer(context) {
        stop()
        onComplete()
    }

    val isInit: Boolean get() = mp.isInit
    val duration: Long get() = mp.duration

    suspend fun init() = mp.init()

    fun release() = mp.release()

    suspend fun CoroutineScope.start(
        lyricsConfig: RhymeLyricsConfig,
        recordImage: ImageBitmap,
        audio: Path
    ) {
        onSceneCreate(Scene(
            rhymeManager = this@RhymeManager,
            lyricsConfig = lyricsConfig,
            recordImage = recordImage
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

    suspend fun CoroutineScope.downloadAssets(): Boolean {
        val imageKeys = arrayOf(
            "left_ui",
            "right_ui",
        )

        val animationKeys = arrayOf<String>(

        )

        val assetList = (imageKeys.map { key ->
            async { key to NetClient.downloadCache(ServerRes.Game.Rhyme.res(key).url)?.let { Asset.decodeImage(it) } }
        } + animationKeys.map { key ->
            async { key to NetClient.downloadCache(ServerRes.Game.Rhyme.res(key).url)?.let { Asset.decodeAnimation(it) } }
        }).awaitAll()

        for ((key, asset) in assetList) assets[key] = asset ?: return false

        assets["body"] = Asset.decodeImage(getDrawableResourceBytes(getSystemResourceEnvironment(), Res.drawable.test))!!
        return true
    }
}