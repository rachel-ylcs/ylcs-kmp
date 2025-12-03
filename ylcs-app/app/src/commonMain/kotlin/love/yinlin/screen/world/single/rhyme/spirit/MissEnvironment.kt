package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.animation.FrameAnimation
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
class MissEnvironment(
    rhymeManager: RhymeManager,
) : Spirit(rhymeManager), BoxBody {
    override val size: Size = manager.size

    private val corners = arrayOf(topLeft, bottomLeft, bottomRight, topRight)
    private val cornerRadius = size.minDimension * 0.75f
    private val brush = Brush.radialGradient(
        colors = listOf(Colors.Red5.copy(alpha = 0.75f), Colors.Transparent),
        center = Offset.Zero,
        radius = cornerRadius
    )

    val animation = object : FrameAnimation(manager.fps * 2, false) {
        override fun calcProgress(t: Int, f: Int): Float {
            val p = (f + 1f) / t
            return 1f - 0.3235f * p * p * p + 1.071f * p * p - 1.7475f * p
        }
    }

    override fun onClientUpdate(tick: Long) {
        animation.update()
    }

    override fun Drawer.onClientDraw() {
        animation.withProgress { progress ->
            for (corner in corners) {
                transform({
                    translate(corner)
                    scale(progress, Offset.Zero)
                }) {
                    circle(brush = brush, position = Offset.Zero, radius = cornerRadius)
                }
            }
        }
    }
}