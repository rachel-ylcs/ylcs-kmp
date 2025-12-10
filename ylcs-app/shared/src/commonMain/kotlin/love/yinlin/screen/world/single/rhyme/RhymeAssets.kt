package love.yinlin.screen.world.single.rhyme

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.io.files.Path
import love.yinlin.api.ServerRes
import love.yinlin.api.url
import love.yinlin.common.downloadCache
import love.yinlin.common.downloadCacheWithPath
import love.yinlin.compose.game.asset.Assets
import love.yinlin.extension.lazyName
import love.yinlin.platform.NetClient

@Stable
class RhymeAssets : Assets() {
    val leftUIBackground by image()
    val rightUIBackground by image()
    val blockMap by image()
    val difficultyStar by image()

    val noteClick by animation()
    val noteDismiss by animation()
    val longPress by animation()

    suspend fun CoroutineScope.init(): Boolean = fetch(
        leftUIBackground,
        rightUIBackground,
        blockMap,
        difficultyStar,
        noteClick,
        noteDismiss,
        longPress
    )

    val soundNoteClick by sound()

    private val sounds = arrayOf(
        soundNoteClick
    )


    private var soundIndex = 0
    private fun sound(version: Int? = null) = lazyName { RhymeSound(version, it, soundIndex++) }

    override suspend fun fetchByteArray(name: String, type: String, version: Int?): ByteArray? {
        val filename = "$name.$type"
        return NetClient.downloadCache("${ServerRes.Game.Rhyme.res(filename).url}?v=$version")
    }

    suspend fun CoroutineScope.initSound(): List<Path> = sounds.map { item ->
        val filename = "${item.name}.wav"
        async { NetClient.downloadCacheWithPath("${ServerRes.Game.Rhyme.res(filename).url}?v=${item.version}")!! }
    }.awaitAll()
}