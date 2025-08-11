package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import love.yinlin.common.Colors
import love.yinlin.extension.translate

// 进度板
@Stable
internal class ProgressBoard(
    imageSet: ImageSet,
    private val duration: Long
) : RhymeDynamic(), RhymeContainer.Circle, RhymeEvent {
    companion object {
        const val STROKE = 8f
        const val RADIUS = 64f
        const val ROTATE_DURATION = 18
        const val APF = 360f / ROTATE_DURATION / RhymeConfig.FPS
    }

    override val position: Offset = Track.Center.translate(-RADIUS, -RADIUS)
    override val size: Size = Size(RADIUS * 2, RADIUS * 2)

    private val record = imageSet.record
    // 封面旋转角
    private var angle: Float by mutableFloatStateOf(0f)
    // 游戏进度
    private var progress: Float by mutableFloatStateOf(0f)

    override fun onUpdate(position: Long) {
        progress = if (duration == 0L) 0f else (position / duration.toFloat()).coerceIn(0f, 1f)
        angle += APF
    }

    override fun onEvent(pointer: Pointer): Boolean = pointer.position in this

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        // 画封面
        rotate(angle, Offset(RADIUS, RADIUS)) { circleImage(record) }
        // 画时长
        arc(Colors.White, -90f, 360f, style = Stroke(width = STROKE, cap = StrokeCap.Round))
        // 画进度
        arc(Colors.Green4, -90f, 360f * progress, style = Stroke(width = STROKE, cap = StrokeCap.Round))
    }
}