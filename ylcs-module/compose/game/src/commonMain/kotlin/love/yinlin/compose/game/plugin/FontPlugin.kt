package love.yinlin.compose.game.plugin

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.LayerOrder

@Stable
class FontPlugin internal constructor(engine: Engine) : Plugin(engine) {
    override val dynamic: Boolean = false
    override val layerOrder: LayerOrder = LayerOrder.Invisible
}