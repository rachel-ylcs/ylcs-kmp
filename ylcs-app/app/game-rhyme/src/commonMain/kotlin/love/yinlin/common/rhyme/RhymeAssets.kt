package love.yinlin.common.rhyme

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import love.yinlin.app
import love.yinlin.compose.game.asset.Assets
import love.yinlin.cs.ServerRes
import love.yinlin.cs.url
import love.yinlin.extension.lazyName
import love.yinlin.fs.File

@Stable
class RhymeAssets : Assets() {
    val leftUIBackground by image()
    val rightUIBackground by image()
    val blockMap by image()
    val difficultyStar by image()

    val noteClick by animation()
    val noteDismiss by animation()
    val longPress by animation()
    val longRelease by animation()

    suspend fun CoroutineScope.init(): Boolean = fetch(
        leftUIBackground,
        rightUIBackground,
        blockMap,
        difficultyStar,
        noteClick,
        noteDismiss,
        longPress,
        longRelease
    )

    val soundNoteClick by sound()

    private val sounds = arrayOf(
        soundNoteClick
    )

    private var soundIndex = 0
    private fun sound(version: Int? = null) = lazyName { RhymeSound(version, it, soundIndex++) }

    override suspend fun fetchByteArray(name: String, type: String, version: Int?): ByteArray? {
        val filename = "$name.$type"
        return app.cache.loadByteArray("${ServerRes.Game.Rhyme.res(filename).url}?v=$version")
    }

    suspend fun CoroutineScope.initSound(): List<File> = sounds.map { item ->
        val filename = "${item.name}.wav"
        async { app.cache.store("${ServerRes.Game.Rhyme.res(filename).url}?v=${item.version}")!! }
    }.awaitAll()
}