package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.fastCoerceAtLeast
import love.yinlin.compose.Colors
import love.yinlin.compose.animation.Interpolator
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.common.BlockTime
import love.yinlin.compose.game.common.InteractStatus
import love.yinlin.compose.game.common.InteractTarget
import love.yinlin.compose.game.data.RhymeDifficulty
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.layer.MapLayer
import love.yinlin.data.music.RhymeAction
import kotlin.math.sin

@Stable
class ContinuedBlock(
    position: Offset,
    line: BlockLine,
    override val time: Time,
    rawIndex: Int,
    lineIndex: Int,
    override val rhymeAction: RhymeAction.Slur,
) : Block<ContinuedBlock.Status>(position, line, rawIndex, lineIndex) {
    @Stable
    data class Time(
        override val appearance: Long,
        val start: Int,
        val end: Int
    ) : BlockTime

    sealed interface Status : BlockStatus {
        class Prepare : Status, BlockStatus.Prepare()
        class InteractStart : Status, BlockStatus.Interact {
            var progress: Float = 0f
        }
        class InteractPressing(val id: Long, val startProgress: Float) : Status, BlockStatus.Interact {
            var tick: Int = 0
            var oscillation: Float = 0f
            var progress: Float = 0f
        }
        class Release(
            val startProgress: Float,
            val endProgress: Float,
            val result: BlockResult
        ) : Status, BlockStatus.Release() {
            override val duration: Int = 500
        }
        class Missing(val startProgress: Float) : Status, BlockStatus.Missing() {
            override val duration: Int = 750
        }
        class Done(val isMissing: Boolean, val result: BlockResult) : Status, BlockStatus.Done
    }

    companion object {
        private const val PERFECT_RATIO = 0.5f
        private const val GOOD_RATIO = 0.25f

        private const val INNER_TIP_ALPHA = 0.3f
        private const val INNER_MAIN_ALPHA = 0.8f
        private val InnerProgressTipColor = Colors.White

        fun buildTime(difficulty: RhymeDifficulty, start: Long, end: Long): Time {
            val prepare = PrepareDurationMap[difficulty]!!
            val duration = (end - start).toInt()
            return Time(
                appearance = start - prepare - PRESS_TOLERANCE,
                start = prepare,
                end = prepare + duration
            )
        }
    }

    private val rawNoteScale: Int = rhymeAction.scale.first().toInt()
    private val scaleIndex: Int = (rawNoteScale - 1) % 7 + 1
    private val scaleLevel: Int = (rawNoteScale - 1) / 7
    private val mainColor: Color = ScaleColorList[scaleIndex]

    override val colorList: List<Color> = listOf(mainColor)

    override fun prepareStatus(): Status = Status.Prepare()

    private fun MapLayer.updateCustomResult(startProgress: Float, endProgress: Float) {
        val progress = endProgress - startProgress
        val result = when {
            progress >= PERFECT_RATIO -> BlockResult.PERFECT
            progress >= GOOD_RATIO -> BlockResult.GOOD
            else -> BlockResult.BAD
        }
        blockStatus = Status.Release(startProgress, endProgress, result)
        updateResult(result)
    }

    override fun onInteract(interactStatusList: List<InteractStatus?>, currentStatus: BlockStatus.Interact) {
        val mapLayer = fromMapLayer ?: return
        when (currentStatus) {
            is Status.InteractStart -> {
                // 只关心按下时刻
                var target: InteractTarget = InteractTarget.None
                for (i in 1 .. 7) {
                    val status = interactStatusList[i]
                    if (status !is InteractStatus.Down) continue
                    target = if (target == InteractTarget.None) InteractTarget.Single(status.id, i) else InteractTarget.Multiple
                }
                val newStatus = when (val interactTarget = target) {
                    is InteractTarget.None -> null // 未按下无事发生
                    is InteractTarget.Multiple -> { // 多指按下以MISS结算
                        mapLayer.updateResult(BlockResult.MISS)
                        Status.Missing(currentStatus.progress)
                    }
                    is InteractTarget.Single -> { // 长按开始
                        if (interactTarget.index == scaleIndex) { // 检查轨道匹配
                            Status.InteractPressing(interactTarget.id, currentStatus.progress)
                        }
                        else { // 按错轨道按MISS结算
                            mapLayer.updateResult(BlockResult.MISS)
                            Status.Missing(currentStatus.progress)
                        }
                    }
                }
                if (newStatus != null) blockStatus = newStatus
            }
            is Status.InteractPressing -> {
                // 只关心抬起时刻, 并且不需要检查其他轨道是否抬起
                val status = interactStatusList[scaleIndex]
                if (status is InteractStatus.Up && status.id == currentStatus.id) {
                    // 抬起结算
                    mapLayer.updateCustomResult(currentStatus.startProgress, currentStatus.progress)
                }
            }
        }
    }

    override fun onUpdate(tick: Int) {
        withMapLayer { mapLayer, audioTick ->
            when (val status = blockStatus) {
                null -> return@withMapLayer false
                is Status.Prepare -> updateCustomPrepare(status, audioTick, time.start, Status::InteractStart)
                is Status.InteractStart -> {
                    val progress = ((audioTick - time.start) / (time.end - time.start).toFloat()).fastCoerceAtLeast(0f)
                    status.progress = progress
                    if (progress >= 1f) {
                        blockStatus = Status.Missing(1f)
                        mapLayer.updateResult(BlockResult.MISS)
                    }
                }
                is Status.InteractPressing -> {
                    val progress = ((audioTick - time.start) / (time.end - time.start).toFloat()).fastCoerceAtLeast(0f)
                    status.progress = progress
                    status.tick += tick
                    status.oscillation = sin(status.tick * 0.02f)
                    if (progress >= 1f) mapLayer.updateCustomResult(status.startProgress, 1f) // 超出时长自动结算
                }
                is Status.Release -> updateCustomRelease(status, tick) { Status.Done(false, it.result) }
                is Status.Missing -> updateCustomRelease(status, tick) { Status.Done(true, BlockResult.MISS) }
                is Status.Done -> return@withMapLayer false
            }
            true
        }
    }

    // 画圆形准备框
    private fun Drawer.drawPrepareCircleBorder(color: Color, progress: Float) {
        val delta = progress * 180f
        arc(color, startAngle = -90f, sweepAngle = delta, Offset.Zero, DefaultSize, style = PrepareStroke)
        arc(color, startAngle = 90f, sweepAngle = delta, Offset.Zero, DefaultSize, style = PrepareStroke)
    }

    // 画最终态的圆形准备框
    private fun Drawer.drawFinalPrepareCircleBorder(color: Color, alpha: Float = 1f) {
        circle(color, DefaultCenter, DEFAULT_RADIUS, alpha, style = PrepareStroke)
    }

    // 画交互旋转块
    private fun Drawer.drawInteractRotateBlock(color: Color, progress: Float, alpha: Float) {
        arc(color, startAngle = -90f, sweepAngle = 360f * progress, Offset.Zero, DefaultSize, alpha = alpha, useCenter = true)
    }

    // 画分段交互旋转块
    private fun Drawer.drawInteractRotateBlock(color1: Color, color2: Color, progress1: Float, progress2: Float, alpha1: Float, alpha2: Float) {
        val angle = 360 * progress1
        arc(color1, startAngle = -90f, sweepAngle = angle, Offset.Zero, DefaultSize, alpha = alpha1, useCenter = true)
        arc(color2, startAngle = -90f + angle, sweepAngle = 360f * progress2 - angle, Offset.Zero, DefaultSize, alpha = alpha2, useCenter = true)
    }

    // 画最终态内部块
    private fun Drawer.drawFinalInnerBlock(color: Color, alpha: Float) {
        circle(color, DefaultCenter, DEFAULT_RADIUS, alpha)
    }

    override fun Drawer.onDraw() {
        withBlockScale {
            when (val status = blockStatus) {
                null -> return
                is Status.Prepare -> {
                    val progress = status.progress

                    drawPrepareCircleBorder(mainColor, progress)
                    drawSingleNoteFont(rawNoteScale, TextColor, Interpolator.decelerate(progress))
                }
                is Status.InteractStart -> {
                    drawInteractRotateBlock(InnerProgressTipColor, status.progress, INNER_TIP_ALPHA)
                    drawFinalPrepareCircleBorder(mainColor)
                    drawSingleNoteFont(rawNoteScale, TextColor, 1f)
                }
                is Status.InteractPressing -> {
                    val oscillation = status.oscillation

                    scale(1.1f + oscillation * 0.1f, DefaultCenter) {
                        drawInteractRotateBlock(InnerProgressTipColor, mainColor, status.startProgress, status.progress, INNER_TIP_ALPHA, INNER_MAIN_ALPHA)
                        drawFinalPrepareCircleBorder(Colors.White, 0.5f + oscillation / 2)
                        drawFinalPrepareCircleBorder(mainColor, 1f - oscillation / 2)
                    }
                    drawSingleNoteFont(rawNoteScale, TextColor, 1f)
                }
                is Status.Release -> {
                    val progress = status.progress
                    val releaseProgress = Interpolator.accelerate(1 - progress)
                    val tipColor = lerp(InnerProgressTipColor, mainColor, progress)

                    withBounceAnimation(progress) {
                        drawBounceAnimation(mainColor) { color, stroke, alpha ->
                            circle(color, DefaultCenter, DEFAULT_RADIUS, style = stroke, alpha = alpha)
                        }
                    }
                    drawInteractRotateBlock(
                        tipColor,
                        mainColor,
                        status.startProgress,
                        Interpolator.map(progress, status.endProgress, 1f),
                        alpha1 = Interpolator.map(progress, INNER_TIP_ALPHA, 1f),
                        alpha2 = Interpolator.map(progress, INNER_MAIN_ALPHA, 1f)
                    )
                    drawFinalPrepareCircleBorder(mainColor, 3 * progress * (progress - 1) + 1)
                    drawSingleNoteFont(rawNoteScale, TextColor, releaseProgress)
                    drawLyricsText(TextColor, (1 - releaseProgress) * LYRICS_TEXT_SCALE)
                }
                is Status.Missing -> {
                    val progress = status.progress
                    val missingProgress = Interpolator.accelerate(1 - progress)
                    val missingColor = lerp(mainColor, MissingColor, progress)

                    drawInteractRotateBlock(missingColor, status.startProgress, alpha = INNER_TIP_ALPHA * missingProgress)
                    drawFinalPrepareCircleBorder(missingColor)
                    drawSingleNoteFont(rawNoteScale, TextColor, missingProgress)
                    drawLyricsText(MissingColor, (1 - missingProgress) * LYRICS_TEXT_SCALE)
                }
                is Status.Done -> {
                    if (!status.isMissing) drawFinalInnerBlock(mainColor, status.result.alpha)
                    drawFinalPrepareCircleBorder(if (status.isMissing) MissingColor else mainColor)
                    drawLyricsText(if (status.isMissing) MissingColor else TextColor, LYRICS_TEXT_SCALE)
                }
            }
        }
    }
}