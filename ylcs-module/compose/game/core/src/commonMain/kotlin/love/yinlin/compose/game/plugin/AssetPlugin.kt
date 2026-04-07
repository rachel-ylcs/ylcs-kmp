package love.yinlin.compose.game.plugin

import androidx.compose.runtime.Stable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.asset.Asset
import love.yinlin.compose.game.asset.AssetLoader
import love.yinlin.compose.game.asset.AssetProvider
import love.yinlin.compose.game.drawer.LayerOrder
import love.yinlin.extension.catchingNull

@Stable
class AssetPlugin private constructor(
    engine: Engine,
    private val loaders: List<AssetLoader<*>>
) : Plugin(engine) {
    /**
     * @param loaders 资源加载器
     */
    @Stable
    class Factory(vararg val loaders: AssetLoader<*>) : PluginFactory {
        override fun build(engine: Engine): Plugin = AssetPlugin(engine, loaders.toList())
    }

    override val layerOrder: Int = LayerOrder.Invisible

    private val assetMap = mutableMapOf<Any, Asset>()

    @Stable
    internal val assetProvider = object : AssetProvider() {
        override fun getAsset(id: Any): Asset? = assetMap[id]
    }

    override suspend fun onInitialize(): Boolean {
        val result = catchingNull {
            coroutineScope {
                buildList {
                    for (loader in loaders) {
                        if (loader.parallel) { // 并行
                            for (assetID in loader.assetIDList) {
                                add(async {
                                    listOf(assetID to loader.load(assetID))
                                })
                            }
                        }
                        else {
                            add(async {
                                val assetList = mutableListOf<Pair<Any, Asset>>()
                                for (assetID in loader.assetIDList) {
                                    ensureActive()
                                    assetList += assetID to loader.load(assetID)
                                }
                                assetList
                            })
                        }
                    }
                }.awaitAll()
            }
        }?.flatten() ?: return false
        assetMap.putAll(result)
        return true
    }

    override fun onRelease() {
        assetMap.clear()
    }
}