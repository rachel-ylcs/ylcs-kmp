package love.yinlin.compose.game.asset

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.lazyName

@Stable
abstract class Assets {
    private val builder = mutableListOf<Asset<*, *>>()
    private val localItems = mutableMapOf<String, Any>()

    protected abstract suspend fun fetchByteArray(name: String, type: String, version: Int?): ByteArray?

    fun image(version: Int? = null): ImageAsset = ImageAsset(version).also { builder += it }
    fun animation(version: Int? = null): AnimationAsset = AnimationAsset(version).also { builder += it }

    protected suspend fun CoroutineScope.fetch(vararg delegates: AssetDelegate<*>): Boolean = catchingDefault(false) {
        for (delegate in delegates) delegate.instance = null
        val result = builder.fastMap { item ->
            val name = item.delegate.name!!
            when (item) {
                is ImageAsset -> async { fetchByteArray(name, item.type, item.version) }
                is AnimationAsset -> async { fetchByteArray(name, item.type, item.version) }
            }
        }.awaitAll()
        builder.fastForEachIndexed { index, asset ->
            val input = result[index]!!
            when (asset) {
                is ImageAsset -> asset.delegate.instance = asset.build(input)
                is AnimationAsset -> asset.delegate.instance = asset.build(input)
            }
        }
        builder.clear()
        true
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(name: String): T = localItems[name] as T
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> invoke() = lazyName { localItems[it] as T }
    operator fun <T : Any> set(name: String, value: T) { localItems[name] = value }
    internal fun clearLocal() = localItems.clear()
}