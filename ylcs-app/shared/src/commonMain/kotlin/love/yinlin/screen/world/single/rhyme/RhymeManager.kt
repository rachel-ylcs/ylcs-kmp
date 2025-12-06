package love.yinlin.screen.world.single.rhyme

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.io.files.Path
import love.yinlin.Context
import love.yinlin.api.ServerRes
import love.yinlin.api.url
import love.yinlin.common.downloadCache
import love.yinlin.compose.game.Asset
import love.yinlin.compose.game.AssetKey
import love.yinlin.compose.game.Manager
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.platform.AudioPlayer
import love.yinlin.platform.NetClient
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

    private val mp = AudioPlayer(context) {
        stop()
        onComplete()
    }

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

        assets["mainRecord"] = Asset.image(recordImage, true)!!

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

    suspend fun CoroutineScope.downloadAssets(): Boolean {
        val imageKeys = arrayOf(
            AssetKey("leftUIBackground"),
            AssetKey("rightUIBackground"),
            AssetKey("blockMap"),
            AssetKey("difficultyStar"),
        )

        val animationKeys = arrayOf<AssetKey>(

        )

        val assetList = (imageKeys.map { (name, version) ->
            async { name to NetClient.downloadCache("${ServerRes.Game.Rhyme.res(name).url}?v=$version")?.let { Asset.image(it) } }
        } + animationKeys.map { (name, version) ->
            async { name to NetClient.downloadCache("${ServerRes.Game.Rhyme.res(name).url}?v=$version")?.let { Asset.animation(it) } }
        }).awaitAll()

        for ((name, asset) in assetList) assets[name] = asset ?: return false

        return true
    }
}