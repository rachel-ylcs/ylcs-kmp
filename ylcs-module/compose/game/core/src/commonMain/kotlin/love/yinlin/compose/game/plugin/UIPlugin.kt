package love.yinlin.compose.game.plugin

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.LayerOrder

@Stable
abstract class UIPlugin(engine: Engine) : Plugin(engine) {
    override val layerOrder: Int = LayerOrder.UI
}