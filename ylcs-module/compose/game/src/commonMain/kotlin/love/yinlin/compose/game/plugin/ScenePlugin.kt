package love.yinlin.compose.game.plugin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMapNotNull
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.Camera
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Entity
import love.yinlin.compose.game.traits.Visible

@Stable
class ScenePlugin internal constructor(engine: Engine) : Plugin(engine) {
    private val camera = Camera()

    private val entities = mutableStateListOf<Entity>()
    private val dynamicEntities by derivedStateOf {
        entities.fastMapNotNull { it as? Dynamic }
    }
    private val visibleEntities by derivedStateOf {
        entities.fastMapNotNull {
            val visible = it as? Visible
            if (visible?.visible == true) visible else null
        }.sortedBy(Visible::zIndex)
    }

    val isEmpty: Boolean get() = entities.isEmpty()
    val isNotEmpty: Boolean get() = entities.isNotEmpty()
    val size: Int get() = entities.size

    operator fun plusAssign(entity: Entity) { entities += entity }

    operator fun minusAssign(entity: Entity) { entities -= entity }

    fun clear() { entities.clear() }

    override fun onUpdate(tick: Long) {
        dynamicEntities.fastForEach { it.onUpdate(tick) }
    }

    @Composable
    override fun BoxScope.Content() {
        Canvas(
            modifier = Modifier.fillMaxSize().onGloballyPositioned {
                camera.updateViewport(it.size.toSize(), engine.viewport)
            }
        ) {
            val drawer = Drawer(this)
            scale(scale = camera.viewportScale, pivot = Offset.Zero) {
                visibleEntities.fastForEach {
                    with(it) {
                        drawer.onDraw()
                    }
                }
            }
        }
    }
}