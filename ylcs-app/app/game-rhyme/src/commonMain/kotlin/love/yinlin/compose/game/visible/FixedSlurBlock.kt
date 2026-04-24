package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import love.yinlin.compose.animation.Interpolator
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.common.BlockTime
import love.yinlin.compose.game.common.InteractStatus
import love.yinlin.compose.game.data.RhymeDifficulty
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.data.music.RhymeAction

@Stable
class FixedSlurBlock(
    position: Offset,
    line: BlockLine,
    override val time: Time,
    rawIndex: Int,
    lineIndex: Int,
    rhymeAction: RhymeAction.Slur,
) : SlurBlock<FixedSlurBlock.Status>(position, line, rawIndex, lineIndex, rhymeAction) {
    @Stable
    data class Time(
        override val appearance: Long,
        val start: Int,
        val end: Int,
        val minPerfectDuration: Int,
        val minGoodDuration: Int
    ) : BlockTime

    sealed interface Status : BlockStatus {
        class Prepare : Status, BlockStatus.Prepare()
        class InteractStart : Status, BlockStatus.Interact {
            var progress: Float = 0f
        }
        class InteractPressing(val lastProgress: Float) : Status, BlockStatus.Interact {
            var progress: Float = 0f
        }
        class Release(val result: BlockResult) : Status, BlockStatus.Release {
            override val duration: Int = 500
            override var progress: Float = 0f
            override var tick: Int = 0
        }
        class Missing : Status, BlockStatus.Missing {
            override val duration: Int = 750
            override var progress: Float = 0f
            override var tick: Int = 0
        }
        class Done(val isMissing: Boolean, val result: BlockResult) : Status, BlockStatus.Done
    }

    companion object {
        private val BlockResultRatioMap = mapOf(
            RhymeDifficulty.Easy to 0.85f,
            RhymeDifficulty.Medium to 0.9f,
            RhymeDifficulty.Hard to 0.95f,
            RhymeDifficulty.Extreme to 1f
        ) // 难度系数
        private const val PERFECT_RATIO = 0.7f
        private const val GOOD_RATIO = 0.4f
        private const val MIN_PRESS_TOLERANCE_RATIO = 10 // 最小交互容忍系数

        fun buildTime(difficulty: RhymeDifficulty, start: Long, end: Long): Time {
            val prepare = PrepareDurationMap[difficulty]!!
            val duration = (end - start).toInt()
            val pressTolerance = duration / MIN_PRESS_TOLERANCE_RATIO
            val actualDuration = pressTolerance + duration
            val difficultyRatio = BlockResultRatioMap[difficulty]!!
            return Time(
                appearance = start - prepare - pressTolerance,
                start = prepare,
                end = prepare + actualDuration,
                minPerfectDuration = (actualDuration * PERFECT_RATIO * difficultyRatio).toInt(),
                minGoodDuration = (actualDuration * GOOD_RATIO * difficultyRatio).toInt()
            )
        }
    }

    private val rawNoteScale: Int = rhymeAction.scale.first().toInt()
    private val scaleIndex: Int = (rawNoteScale - 1) % 7 + 1
    private val scaleLevel: Int = (rawNoteScale - 1) / 7
    private val mainColor: Color = ScaleColorList[scaleIndex]

    override val colorList: List<Color> = listOf(mainColor)

    override fun prepareStatus(): Status = Status.Prepare()

    override fun onInteract(interactStatusList: List<InteractStatus?>, currentStatus: BlockStatus.Interact) {

    }

    override fun onUpdate(tick: Int) {
        withMapLayer { mapLayer, audioTick ->
            when (val status = blockStatus) {
                null -> return@withMapLayer false
                is Status.Prepare -> updateCustomPrepare(status, audioTick, time.start, Status::InteractStart)
                is Status.InteractStart -> {
                    val progress = ((audioTick - time.start) / (time.end - time.start).toFloat()).coerceAtLeast(0f)
                    status.progress = progress
                    if (progress >= 1f) {
                        blockStatus = Status.Missing()
                        mapLayer.updateResult(BlockResult.MISS)
                    }
                }
                is Status.InteractPressing -> {
                    val progress = ((audioTick - time.start) / (time.end - time.start).toFloat()).coerceAtLeast(0f)
                    status.progress = progress
                    if (progress >= 1f) {

                    }
                }
                is Status.Release -> updateCustomRelease(status, tick) { Status.Done(false, it.result) }
                is Status.Missing -> updateCustomRelease(status, tick) { Status.Done(true, BlockResult.MISS) }
                is Status.Done -> return@withMapLayer false
            }
            true
        }
    }

    override fun Drawer.onDraw() {
        withBlockScale {
            when (val status = blockStatus) {
                null -> return
                is Status.Prepare -> {
//                    val progress = status.progress
//
//                    drawPrepareBorder(mainColor, progress)
//                    scale(INNER_BORDER_SCALE, DefaultCenter) { drawPrepareBorder(mainColor, progress, alpha = INNER_BORDER_ALPHA) }
//                    drawSingleNoteFont(rawNoteScale, TextColor, Interpolator.decelerate(progress))
                }
                is Status.InteractStart -> {

                }
                is Status.InteractPressing -> {
//                    drawScaleBlock(mainColor, status.progress)
//                    drawFullPrepareBorder(mainColor)
//                    scale(INNER_BORDER_SCALE, DefaultCenter) { drawFullPrepareBorder(mainColor, alpha = INNER_BORDER_ALPHA) }
//                    drawSingleNoteFont(rawNoteScale, TextColor, 1f)
                }
                is Status.Release -> {
//                    val progress = status.progress
//                    val releaseProgress = Interpolator.accelerate(1 - progress)
//                    val borderAlpha = 3 * progress * (progress - 1) + 1
//
//                    drawBounceBorder(mainColor, 1.875f * progress * (1 - progress) + 1)
//                    drawScaleBlock(mainColor, releaseProgress * status.lastProgress)
//                    drawFullPrepareBorder(mainColor, borderAlpha)
//                    scale(INNER_BORDER_SCALE, DefaultCenter) { drawFullPrepareBorder(mainColor, alpha = borderAlpha * INNER_BORDER_ALPHA) }
//                    drawSingleNoteFont(rawNoteScale, TextColor, releaseProgress)
//                    drawLyricsText(TextColor, Interpolator.decelerate(progress) * LYRICS_TEXT_SCALE)
                }
                is Status.Missing -> {
//                    val progress = status.progress
//                    val missingProgress = Interpolator.accelerate(1 - progress)
//                    val missingColor = lerp(mainColor, MissingColor, progress)
//
//                    drawScaleBlock(missingColor, missingProgress)
//                    drawFullPrepareBorder(missingColor)
//                    scale(INNER_BORDER_SCALE, DefaultCenter) { drawFullPrepareBorder(missingColor, alpha = INNER_BORDER_ALPHA) }
//                    drawSingleNoteFont(rawNoteScale, TextColor, missingProgress)
//                    drawLyricsText(MissingColor, Interpolator.decelerate(progress) * LYRICS_TEXT_SCALE)
                }
                is Status.Done -> {
//                    val borderColor = if (status.isMissing) MissingColor else mainColor
//
//                    drawPrepareBorder(borderColor, 1f)
//                    scale(INNER_BORDER_SCALE, DefaultCenter) { drawFullPrepareBorder(borderColor, alpha = INNER_BORDER_ALPHA) }
//                    drawLyricsText(if (status.isMissing) MissingColor else TextColor, LYRICS_TEXT_SCALE)
                }
            }
        }
    }
}