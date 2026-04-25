package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import love.yinlin.compose.Colors
import love.yinlin.compose.animation.Interpolator
import love.yinlin.compose.extension.Path
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.common.BlockTime
import love.yinlin.compose.game.common.InteractStatus
import love.yinlin.compose.game.data.RhymeDifficulty
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.layer.MapLayer
import love.yinlin.data.music.RhymeAction

@Stable
class OffsetSlurBlock(
    position: Offset,
    line: BlockLine,
    override val time: Time,
    rawIndex: Int,
    lineIndex: Int,
    override val rhymeAction: RhymeAction.Slur,
) : Block<OffsetSlurBlock.Status>(position, line, rawIndex, lineIndex) {
    @Stable
    data class Time(
        override val appearance: Long,
        val start: Int,
        val end: Int
    ) : BlockTime

    sealed interface Status : BlockStatus {
        class Prepare : Status, BlockStatus.Prepare()
        class Interact(size: Int) : Status, BlockStatus.Interact {
            var progress: Float = 0f
            var noteProgressList = MutableList<Float?>(size) { null }
        }
        class Release(
            val lastProgress: Float,
            val noteProgressList: List<Float?>,
            val result: BlockResult
        ) : Status, BlockStatus.Release() {
            override val duration: Int = 500
        }
        class Missing(
            val lastProgress: Float,
            val noteProgressList: List<Float?>
        ) : Status, BlockStatus.Missing() {
            override val duration: Int = 750
        }
        class Done(val isMissing: Boolean, val result: BlockResult) : Status, BlockStatus.Done
    }

    companion object {
        private const val INTERACT_NEW_NOTE_DURATION = 200 // 新音符添加动画时长

        private const val INNER_ROTATE_BLOCK_ALPHA = 0.3f
        private const val ROTATE_BLOCK_ALPHA = 0.8f
        private val InnerProgressTipColor = Colors.White
        private val DefaultPath = Path(arrayOf(TopCenter, CenterRight, BottomCenter, CenterLeft))

        fun buildTime(difficulty: RhymeDifficulty, start: Long, end: Long): Time {
            val prepare = PrepareDurationMap[difficulty]!!
            val duration = maxOf((end - start).toInt(), prepare / 2)
            return Time(
                appearance = start - prepare - PRESS_TOLERANCE,
                start = prepare,
                end = prepare + duration
            )
        }
    }

    private val rawNoteScaleList: List<Int> = rhymeAction.scale.fastMap { it.toInt() }
    private val scaleIndexs: List<Int> = rawNoteScaleList.fastMap { (it - 1) % 7 + 1 }
    private val scaleLevels: List<Int> = rawNoteScaleList.fastMap { (it - 1) / 7 }

    override val colorList: List<Color> = scaleIndexs.fastMap { ScaleColorList[it] }
    private val colorBrush: Brush = Brush.sweepGradient(colorList, center = DefaultCenter)

    override fun prepareStatus(): Status = Status.Prepare()

    private fun MapLayer.updateCustomResult(lastProgress: Float, noteProgressList: List<Float?>) {
        val num = noteProgressList.count { it != null }
        val count = scaleIndexs.size
        val result = when (num) {
            count -> BlockResult.PERFECT
            count - 1 -> BlockResult.GOOD
            0 -> BlockResult.MISS
            else -> BlockResult.BAD
        }
        blockStatus = if (result == BlockResult.MISS) Status.Missing(lastProgress, noteProgressList) else Status.Release(lastProgress, noteProgressList, result)
        updateResult(result)
    }

    override fun onInteract(interactStatusList: List<InteractStatus?>, currentStatus: BlockStatus.Interact) {

    }

    override fun onUpdate(tick: Int) {
        withMapLayer { mapLayer, audioTick ->
            when (val status = blockStatus) {
                null -> return@withMapLayer false
                is Status.Prepare -> updateCustomPrepare(status, audioTick, time.start) { Status.Interact(scaleIndexs.size) }
                is Status.Interact -> {
                    val progress = ((audioTick - time.start) / (time.end - time.start).toFloat()).fastCoerceAtLeast(0f)
                    status.progress = progress
                    status.noteProgressList.fastForEachIndexed { index, noteProgress ->
                        if (noteProgress != null) status.noteProgressList[index] = (noteProgress + tick / INTERACT_NEW_NOTE_DURATION).fastCoerceAtMost(1f)
                    }
                    if (progress >= 1f) mapLayer.updateCustomResult(1f, status.noteProgressList)
                }
                is Status.Release -> updateCustomRelease(status, tick) { Status.Done(false, it.result) }
                is Status.Missing -> updateCustomRelease(status, tick) { Status.Done(true, BlockResult.MISS) }
                is Status.Done -> return@withMapLayer false
            }
            true
        }
    }

    // 画菱形准备框
    private fun Drawer.drawPrepareDiamondBorder(progress: Float) {
        val delta = progress * DEFAULT_RADIUS
        val deltaInv = DEFAULT_RADIUS - delta

        line(colorBrush, TopCenter, Offset(deltaInv, delta), style = PrepareStroke)
        line(colorBrush, TopCenter, Offset(DEFAULT_DIMENSION - deltaInv, delta), style = PrepareStroke)
        line(colorBrush, BottomCenter, Offset(deltaInv, DEFAULT_DIMENSION - delta), style = PrepareStroke)
        line(colorBrush, BottomCenter, Offset(DEFAULT_DIMENSION - deltaInv, DEFAULT_DIMENSION - delta), style = PrepareStroke)
    }

    // 画最终态菱形准备框
    private fun Drawer.drawFinalPrepareDiamondBorder(alpha: Float = 1f) {
        path(colorBrush, DefaultPath, alpha = alpha, style = PrepareStroke)
    }

    private fun Drawer.drawFinalPrepareDiamondBorder(color: Color, alpha: Float = 1f) {
        path(color, DefaultPath, alpha = alpha, style = PrepareStroke)
    }

    // 画交互旋转块
    private inline fun Drawer.drawInteractRotateBlock(color: Color, noteColorProvider: (Int) -> Color, progress: Float, noteProgressList: List<Float?>, alpha1: Float, alpha2: Float) {
        val perAngle = 360f / noteProgressList.size

        clip(DefaultPath) {
            arc(color, startAngle = -90f, sweepAngle = 360f * progress, Offset.Zero, DefaultSize, alpha = alpha1, useCenter = true)
            noteProgressList.forEachIndexed { index, noteProgress ->
                if (noteProgress != null) {
                    val noteColor = noteColorProvider(index)
                    arc(noteColor, startAngle = -90f + index * perAngle, sweepAngle = perAngle * noteProgress, Offset.Zero, DefaultSize, alpha = alpha2, useCenter = true)
                }
            }
        }
    }

    // 画最终态内部
    private fun Drawer.drawFinalInnerBlock(alpha: Float) {
        path(colorBrush, DefaultPath, alpha = alpha)
    }

    override fun Drawer.onDraw() {
        withBlockScale {
            when (val status = blockStatus) {
                null -> return
                is Status.Prepare -> {
                    val progress = status.progress

                    drawPrepareDiamondBorder(progress)
                    drawMultipleNoteFont(rawNoteScaleList, TextColor, Interpolator.decelerate(progress))
                }
                is Status.Interact -> {
                    drawInteractRotateBlock(
                        InnerProgressTipColor,
                        { colorList[it] },
                        status.progress,
                        status.noteProgressList,
                        alpha1 = INNER_ROTATE_BLOCK_ALPHA,
                        alpha2 = ROTATE_BLOCK_ALPHA
                    )
                    drawFinalPrepareDiamondBorder()
                    drawMultipleNoteFont(rawNoteScaleList, TextColor, 1f)
                }
                is Status.Release -> {

                }
                is Status.Missing -> {
                    val progress = status.progress
                    val missingProgress = Interpolator.accelerate(1 - progress)
                    val tipColor = lerp(InnerProgressTipColor, MissingColor, progress)

                    drawInteractRotateBlock(
                        tipColor,
                        noteColorProvider = { lerp(colorList[it], MissingColor, progress) },
                        progress = progress,
                        noteProgressList = status.noteProgressList,
                        alpha1 = INNER_ROTATE_BLOCK_ALPHA * missingProgress,
                        alpha2 = ROTATE_BLOCK_ALPHA * missingProgress
                    )
                }
                is Status.Done -> {
                    if (status.isMissing) {
                        drawFinalPrepareDiamondBorder(MissingColor)
                        drawLyricsText(MissingColor, LYRICS_TEXT_SCALE)
                    }
                    else {
                        drawFinalInnerBlock(status.result.alpha)
                        drawFinalPrepareDiamondBorder()
                        drawLyricsText(TextColor, LYRICS_TEXT_SCALE)
                    }
                }
            }
        }
    }
}