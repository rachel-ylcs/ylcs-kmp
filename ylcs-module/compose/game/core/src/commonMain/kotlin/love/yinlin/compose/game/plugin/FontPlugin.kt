package love.yinlin.compose.game.plugin

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.util.fastForEach
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.FontProvider
import love.yinlin.compose.game.common.LayerOrder
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

    override val layerOrder: Int = LayerOrder.Low

    private val fontMap = mutableStateMapOf<FontResource, FontFamily>()

    @Stable
    internal val fontProvider = FontProvider { resource ->
        resource?.let { fontMap[it] } ?: FontFamily.Default
    }

    override fun onRelease() {
        fontMap.clear()
    }

    @Composable
    override fun BoxScope.Content() {
        fontResources.fastForEach { fontResource ->
            key(fontResource) {
                val font = Font(fontResource)

                LaunchedEffect(fontResource, font) {
                    fontMap[fontResource] = FontFamily(font)
                }
            }
        }
    }
}