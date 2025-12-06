package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.traits.CircleBody
import love.yinlin.compose.game.traits.Event
import love.yinlin.compose.game.traits.PointerUpEvent
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.Transform
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
class RecordContainer(
    private val rhymeManager: RhymeManager
) : Spirit(rhymeManager), CircleBody {
    override val preTransform: List<Transform> = listOf(Transform.Translate(-28f, -44f))
    override val size: Size = Size(236f, 236f)

    private val mainRecord: ImageBitmap by manager.assets()

    // 封面旋转角
    private var angle: Float by mutableFloatStateOf(0f)
    private val anglePerTick = 360f / manager.fps / 18

    override fun onClientUpdate(tick: Long) {
        var newAngle = angle + anglePerTick
        if (newAngle >= 360f) newAngle -= 360f
        angle = newAngle
    }

    override fun onClientEvent(tick: Long, event: Event): Boolean {
        if (event is PointerUpEvent && event.rawPosition in this) rhymeManager.onPause()
        return true
    }

    override fun Drawer.onClientDraw() {
        rotate(angle) {
            // 画封面
            circleImage(mainRecord)
            // 画挖孔
            circle(Colors.Black, center, 15f)
            circle(Colors.Gray4, center, 15f, style = Stroke(width = 2f))
        }
    }
}