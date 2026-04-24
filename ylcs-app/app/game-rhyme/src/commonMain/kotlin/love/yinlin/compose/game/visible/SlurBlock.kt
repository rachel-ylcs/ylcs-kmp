package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
        const val INNER_ROTATE_BLOCK_ALPHA = 0.5f
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
    protected fun Drawer.drawInteractRotateBlock(color: Color, progress: Float) {
        arc(color, startAngle = -90f, sweepAngle = 360f * progress, Offset.Zero, DefaultSize, alpha = INNER_ROTATE_BLOCK_ALPHA, useCenter = true)
    }
}