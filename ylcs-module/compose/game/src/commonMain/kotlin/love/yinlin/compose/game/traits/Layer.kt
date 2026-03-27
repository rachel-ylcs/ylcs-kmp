package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.common.Drawer

@Stable
abstract class Layer : Visible() {
    abstract fun Drawer.onLayerDraw()

    final override fun Drawer.onDraw() {

    }
}