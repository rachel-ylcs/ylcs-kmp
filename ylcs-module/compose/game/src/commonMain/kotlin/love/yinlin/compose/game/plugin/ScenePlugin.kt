package love.yinlin.compose.game.plugin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastMapNotNull
import androidx.compose.ui.zIndex
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.Camera
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Entity
import love.yinlin.compose.game.traits.Layer

@Stable
class ScenePlugin internal constructor(engine: Engine) : Plugin(engine) {
    val camera = Camera()

    private val entities = mutableStateListOf<Entity>()
    private val dynamicEntities by derivedStateOf {
        entities.fastMapNotNull {
            val dynamic = it as? Dynamic
            if (dynamic?.active == true) dynamic else null
        }
    }
    private val layerEntities by derivedStateOf {
        entities.fastMapNotNull {
            val layer = it as? Layer
            if (layer?.visible == true) layer else null
        }.sortedBy(Layer::zIndex)
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

    override suspend fun onInitialize() {
        if (!isInitialized) {
            entities.fastForEach { it.onAttached(engine) }
            isInitialized = true
        }
    }

    override fun onRelease() {
        clear()
        isInitialized = false
    }

    override fun onUpdate(tick: Long) {
        dynamicEntities.fastForEach { it.onUpdate(tick) }
    }

    @Composable
    override fun BoxScope.Content() {
        Box(modifier = Modifier.fillMaxSize().onSizeChanged {
            camera.updateViewport(it.toSize(), engine.viewport)
        }.clipToBounds().graphicsLayer {
            camera.transformLayer(this, size)
        }) {
            layerEntities.fastForEach { layer ->
                key(layer.id) {
                    val drawer = remember { Drawer() }

                    Box(modifier = Modifier.fillMaxSize().drawWithCache {
                        val bounds = camera.viewportBounds
                        val viewportSize = camera.viewportSize
                        onDrawBehind {
                            drawer.scope = this
                            with(layer) {
                                drawer.drawVisibleLayer(viewportSize, bounds)
                            }
                            drawer.scope = null
                        }
                    }.zIndex(layer.zIndex.toFloat()))
                }
            }
        }
    }
}