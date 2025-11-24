package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.compose.game.Asset
import love.yinlin.compose.game.BoxBody
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.Manager
import love.yinlin.compose.game.Spirit
import love.yinlin.compose.game.traits.Dynamic

class LeftUI(
    manager: Manager,
    private val record: ImageBitmap
) : Spirit(manager), Dynamic {
    override val box = BoxBody(Offset.Zero, Size(600f, 200f))

    private val backgorund = (manager.assets["left_ui"] as Asset.Image).image

    // 封面旋转角
    private var angle: Float by mutableFloatStateOf(0f)

    override fun Drawer.onDraw() {
        // 画背景
        image(backgorund)
        // 画封面
        // rotate(angle, Offset())
        transform({
            translate(Offset(-20f, -20f))
        }) {
            circleImage(record, Offset.Zero, Size(200f, 200f))
        }
    }

    override fun onUpdate(tick: Long) {

    }
}