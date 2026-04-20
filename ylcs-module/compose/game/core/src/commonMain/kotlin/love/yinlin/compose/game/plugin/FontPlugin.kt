package love.yinlin.compose.game.plugin

import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontFamily
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.font.FontProvider
import love.yinlin.compose.game.drawer.LayerOrder
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.SyncFuture
import love.yinlin.extension.replaceAll
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

@Stable
class FontPlugin private constructor(
    engine: Engine,
    private val fontResources: List<FontResource>
) : Plugin(engine) {
    /**
     * @param font 字体资源
     */
    @Stable
    class ResourceFactory(vararg val font: FontResource) : PluginFactory {
        override fun build(engine: Engine): Plugin = FontPlugin(engine, font.toList())
    }

    override val layerOrder: Int = LayerOrder.Invisible

    private val fontMap = mutableMapOf<FontResource, FontFamily>()

    @Stable
    internal val fontProvider = FontProvider { resource -> fontMap[resource] ?: FontFamily.Default }

    private var initializeFuture: SyncFuture<Boolean>? by mutableStateOf(null)

    override suspend fun onInitialize(): Boolean {
        val result = Coroutines.sync<Boolean> { initializeFuture = it } ?: false
        initializeFuture = null
        return result
    }

    override fun onRelease() {
        fontMap.clear()
    }

    override val preloadEnvironment: @Composable () -> Unit = {
        val newFontMap = fontResources.associateWith { FontFamily(Font(it)) }

        LaunchedEffect(newFontMap, initializeFuture) {
            initializeFuture?.let { future ->
                fontMap.replaceAll(newFontMap)
                future.send(true)
            }
        }
    }
}