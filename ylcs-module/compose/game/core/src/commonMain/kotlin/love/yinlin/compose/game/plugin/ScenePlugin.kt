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
import love.yinlin.compose.extension.rememberDerivedState
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

    private val dynamicEntities by derivedStateOf {
        entities.fastMapNotNull { it as? Dynamic }
    }

    private val layerEntities by derivedStateOf {
        entities.fastMapNotNull { it as? Layer }.sortedBy(Layer::layerOrder)
    }

    val isEmpty: Boolean get() = entities.isEmpty()
    val isNotEmpty: Boolean get() = entities.isNotEmpty()
    val entityCount: Int get() = entities.size

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
        dynamicEntities.fastForEach {
            if (it.active) it.onUpdate(tick)
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
            val fontProvider = remember { engine.pluginOrNull<FontPlugin>()?.fontProvider ?: FontProvider.Default }
            val fontFamilyResolver = LocalFontFamilyResolver.current

            val cameraState by rememberDerivedState {
                val _ = camera.requireDirty
                camera.viewportSize to camera.viewportBounds
            }

            layerEntities.fastForEach { layer ->
                key(layer.id) {
                    val drawer = remember(fontFamilyResolver) {
                        Drawer(
                            fontFamilyResolver = fontFamilyResolver,
                            fontProvider = fontProvider
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                        val (viewportSize, bounds) = cameraState
                        val _ = layer.requireDirty
                        val layerVisible = layer.visible

                        // 预绘制处理
                        drawer.withRawCacheScope(this) {
                            with(layer) {
                                if (layerVisible) drawer.prepareDrawVisibleLayer(viewportSize, bounds)
                            }
                        }

                        // 绘制
                        onDrawBehind {
                            drawer.withRawScope(this) {
                                with(layer) {
                                    if (layerVisible) drawer.drawVisibleLayer()
                                }
                            }
                        }
                    }.zIndex(layer.layerOrder.toFloat()))
                }
            }
        }
    }
}