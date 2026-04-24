package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.Colors
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.data.music.RhymeAction

@Stable
abstract class SlurBlock<BS : BlockStatus>(
    position: Offset,
    line: BlockLine,
    rawIndex: Int,
    lineIndex: Int,
    override val rhymeAction: RhymeAction.Slur,
) : Block<BS>(position, line, rawIndex, lineIndex) {
    companion object {
        protected const val INNER_ROTATE_BLOCK_ALPHA = 0.3f
        protected const val ROTATE_BLOCK_ALPHA = 0.8f
        protected val InnerProgressTipColor = Colors.White
    }

    // 画圆形准备框
    protected fun Drawer.drawPrepareBorder(color: Color, progress: Float) {
        val delta = progress * 180f
        arc(color, startAngle = -90f, sweepAngle = delta, Offset.Zero, DefaultSize, style = PrepareStroke)
        arc(color, startAngle = 90f, sweepAngle = delta, Offset.Zero, DefaultSize, style = PrepareStroke)
    }

    // 画最终态的圆形准备框
    protected fun Drawer.drawFullPrepareBorder(color: Color, alpha: Float = 1f) {
        circle(color, DefaultCenter, DEFAULT_RADIUS, alpha, style = PrepareStroke)
    }

    // 画交互旋转块
    protected fun Drawer.drawInteractRotateBlock(color: Color, progress: Float, alpha: Float) {
        arc(color, startAngle = -90f, sweepAngle = 360f * progress, Offset.Zero, DefaultSize, alpha = alpha, useCenter = true)
    }

    // 画分段交互旋转块
    protected fun Drawer.drawInteractRotateBlock(color1: Color, color2: Color, progress1: Float, progress2: Float, alpha1: Float, alpha2: Float) {
        val angle = 360 * progress1
        arc(color1, startAngle = -90f, sweepAngle = angle, Offset.Zero, DefaultSize, alpha = alpha1, useCenter = true)
        arc(color2, startAngle = -90f + angle, sweepAngle = 360f * progress2 - angle, Offset.Zero, DefaultSize, alpha = alpha2, useCenter = true)
    }

    // 画弹出边框动画
    protected fun Drawer.drawBounceBorder(color: Color, ratio: Float) {
        scale(ratio, DefaultCenter) {
            circle(color, DefaultCenter, DEFAULT_RADIUS, style = BounceBorderStroke[0], alpha = 0.2f)
            circle(color, DefaultCenter, DEFAULT_RADIUS, style = BounceBorderStroke[1], alpha = 0.5f)
            circle(color, DefaultCenter, DEFAULT_RADIUS, style = BounceBorderStroke[2], alpha = 0.9f)
            circle(Colors.White, DefaultCenter, DEFAULT_RADIUS, style = BounceBorderStroke[3], alpha = 0.4f)
            circle(Colors.White, DefaultCenter, DEFAULT_RADIUS, style = BounceBorderStroke[4], alpha = 0.8f)
        }
    }
}