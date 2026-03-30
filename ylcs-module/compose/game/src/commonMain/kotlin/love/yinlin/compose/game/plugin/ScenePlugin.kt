package love.yinlin.compose.game.plugin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastMapNotNull
import androidx.compose.ui.zIndex
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.Camera
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.common.FontProvider
import love.yinlin.compose.game.common.LayerOrder
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Entity
import love.yinlin.compose.game.traits.Layer

@Stable
class ScenePlugin(engine: Engine) : Plugin(engine) {
    val camera = Camera()

    private val entities = mutableStateListOf<Entity>()

    private val layerEntities by derivedStateOf {
        entities.fastMapNotNull {
            val layer = it as? Layer
            if (layer?.visible == true) layer else null
        }.sortedBy(Layer::layerOrder)
    }

    val isEmpty: Boolean get() = entities.isEmpty()
    val isNotEmpty: Boolean get() = entities.isNotEmpty()
    val size: Int get() = entities.size

    operator fun plusAssign(entity: Entity) {
        entities += entity
        if (isInitialized) entity.onAttached(engine)
    }

    operator fun minusAssign(entity: Entity) {
        entities -= entity
        if (isInitialized) entity.onDetached(engine)
    }

    fun clear() {
        if (isInitialized) {
            entities.clear()
            entities.fastForEachReversed { it.onDetached(engine) }
        }
    }

    override fun onRelease() = clear()

    override fun onUpdate(tick: Long) {
        entities.fastForEach {
            if (it is Dynamic && it.active) it.onUpdate(tick)
        }
    }

    override val layerOrder: Int = LayerOrder.GameSurface

    @Composable
    override fun BoxScope.Content() {
        Box(modifier = Modifier.fillMaxSize().onSizeChanged {
            camera.updateViewport(it, engine.viewport)
        }.graphicsLayer {
            val _ = camera.requireDirty
            camera.transformLayer(this, size)
        }) {
            val fontFamilyResolver = LocalFontFamilyResolver.current

            layerEntities.fastForEach { layer ->
                key(layer.id) {
                    val drawer = remember(fontFamilyResolver) {
                        Drawer(
                            textCacheCapacity = layer.textCacheCapacity,
                            fontFamilyResolver = fontFamilyResolver,
                            fontProvider = engine.pluginOrNull<FontPlugin>()?.fontProvider ?: FontProvider.Default
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                        val _ = camera.requireDirty
                        val bounds = camera.viewportBounds
                        val viewportSize = camera.viewportSize

                        // 预绘制处理
                        with(layer) { drawer.prepareDraw(viewportSize, bounds) }

                        // 实际绘制
                        onDrawBehind {
                            val rawScope = this
                            drawer.rawScope = rawScope
                            with(layer) {
                                drawer.drawVisibleLayer(rawScope, bounds)
                            }
                            drawer.rawScope = null
                        }
                    }.zIndex(layer.layerOrder.toFloat()))
                }
            }
        }
    }
}