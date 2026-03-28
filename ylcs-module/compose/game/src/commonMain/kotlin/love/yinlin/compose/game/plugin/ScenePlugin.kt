package love.yinlin.compose.game.plugin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
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
    val camera = Camera()

    private val entities = mutableStateListOf<Entity>()
    private val dynamicEntities by derivedStateOf {
        entities.fastMapNotNull {
            val dynamic = it as? Dynamic
            if (dynamic?.active == true) dynamic else null
        }
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
        Box(modifier = Modifier.fillMaxSize().drawWithCache {
            val drawer = Drawer(camera.updateViewport(size, engine.viewport))

            onDrawBehind {
                val rawScope = this
                val bounds = camera.viewportBounds
                drawer.draw(rawScope) {
                    visibleEntities.fastForEach { drawer.drawVisible(rawScope, bounds, it) }
                }
            }
        })
    }
}