package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.Colors
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.LayerType
import love.yinlin.compose.game.event.Event
import love.yinlin.compose.game.event.PointerEventListener
import love.yinlin.compose.game.plugin.ScenePlugin
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.traits.Trigger
import love.yinlin.compose.game.traits.Visible

@Stable
open class BackgroundLayer : Layer(layerOrder = 0, layerType = LayerType.Absolute), Dynamic {
    private val background = object : Visible(size = Size(300f, 300f)) {
        override val trigger: Trigger = Trigger(
            object : PointerEventListener() {
                override fun onPointerDown(tick: Int, event: Event.Pointer.Down) {
                    println("hit me")
                }
            }
        )
        override fun Drawer.onDraw() {
            rect(Colors.White, Offset.Zero, size)
        }
    }

    override fun onLayerAttached(scene: ScenePlugin) {
        this += background
    }

    override fun onLayerDetached(sender: ScenePlugin) {
        this -= background
    }
}