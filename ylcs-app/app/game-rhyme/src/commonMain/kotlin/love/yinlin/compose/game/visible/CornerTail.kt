package love.yinlin.compose.game.visible

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import love.yinlin.compose.game.common.BlockDirection
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Visible

class CornerTail private constructor(
    position: Offset,
    private val color: Color,
    private val startAngle: Float,
    endAngle: Float
) : Visible(position = position, size = DefaultSize, useCulling = false), Dynamic {
    companion object {
        const val ANIMATION_DURATION = 750
        val DefaultSize = Block.DefaultSize * 2f
        val ArcStroke = Stroke(20f, cap = StrokeCap.Round)

        fun build(block: Block<*>, startDirection: BlockDirection, endDirection: BlockDirection): CornerTail {
            val position = block.position
            val color = block.colorList.random()
            return when (startDirection) {
                BlockDirection.UP -> when (endDirection) {
                    BlockDirection.LEFT -> CornerTail(position, color, 30f, -120f)
                    BlockDirection.RIGHT -> CornerTail(position, color, 150f, 300f)
                    else -> error("")
                }
                BlockDirection.DOWN -> when (endDirection) {
                    BlockDirection.LEFT -> CornerTail(position, color, -30f, 120f)
                    BlockDirection.RIGHT -> CornerTail(position, color, 210f, 60f)
                    else -> error("")
                }
                BlockDirection.LEFT -> when (endDirection) {
                    BlockDirection.UP -> CornerTail(position, color, 60f, 210f)
                    BlockDirection.DOWN -> CornerTail(position, color, 300f, 150f)
                    else -> error("")
                }
                BlockDirection.RIGHT -> when (endDirection) {
                    BlockDirection.UP -> CornerTail(position, color, 120f, -30f)
                    BlockDirection.DOWN -> CornerTail(position, color, -120f, 30f)
                    else -> error("")
                }
            }
        }
    }

    override val clip: Boolean = false
    override val layerOrder: Int = 0

    private val angleDuration = endAngle - startAngle

    private var isRemove: Boolean = false
    private var time: Int = 0
    private var progress: Float = 0f

    override fun onUpdate(tick: Int) {
        if (time < ANIMATION_DURATION) {
            time += tick
            val t = (time / ANIMATION_DURATION.toFloat()).coerceIn(0f, 1f)
            progress = 1 - (1 - t) * (1 - t)
        }
        else if (!isRemove) {
            isRemove = true
            layer?.removeAfterUpdate(this)
        }
    }

    override fun Drawer.onDraw() {
        val head = (progress / 0.7f).coerceAtMost(1f)
        val tail = ((progress - 0.3f) / 0.7f).coerceAtLeast(0f)
        val start = startAngle + tail * angleDuration
        val sweep = (head - tail) * angleDuration

        arc(color, start, sweep, Offset.Zero, DefaultSize, style = ArcStroke, alpha = 1 - progress)
    }
}