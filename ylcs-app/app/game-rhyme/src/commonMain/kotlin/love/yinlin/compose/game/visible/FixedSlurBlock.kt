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
    override val rhymeAction: RhymeAction.Slur,
) : Block<FixedSlurBlock.Status>(position, line, rawIndex, lineIndex) {
    @Stable
    data class Time(
        override val appearance: Long,
        val start: Int,
        val end: Int
    ) : BlockTime

    interface Status : BlockStatus {
        class Prepare : Status, BlockStatus.Prepare {
            var progress: Float = 0f
        }
        class Interact : Status, BlockStatus.Interact {
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
        fun buildTime(difficulty: RhymeDifficulty, start: Long, end: Long): Time {
            val prepare = PrepareDurationMap[difficulty]!!
            return Time(
                appearance = start - prepare - LONG_PRESS_TOLERANCE,
                start = prepare,
                end = (prepare + MIN_LONG_PRESS_DURATION + LONG_PRESS_TOLERANCE + end - start).toInt()
            )
        }
    }

    private val rawNoteScale: Int = rhymeAction.scale.first().toInt()
    private val scaleIndex: Int = (rawNoteScale - 1) % 7 + 1
    private val scaleLevel: Int = (rawNoteScale - 1) / 7
    private val mainColor: Color = ScaleColorList[scaleIndex]

    override val colorList: List<Color> = listOf(mainColor)

    override fun prepareStatus(): Status = Status.Prepare()

    override fun onInteract(interactStatus: Array<InteractStatus>, currentStatus: BlockStatus.Interact) {

    }

    override fun onUpdate(tick: Int) {
        withMapLayer { mapLayer, audioTick ->
            when (val status = blockStatus) {
                null -> return@withMapLayer false
                is Status.Prepare -> {
                    if (audioTick >= time.start) blockStatus = Status.Interact()
                    else status.progress = (audioTick / time.start.toFloat()).coerceIn(0f, 1f)
                }
                is Status.Interact -> {
                    val progress = ((audioTick - time.start) / (time.end - time.start).toFloat()).coerceAtLeast(0f)
                    status.progress = progress
                    if (progress >= 1f) {
                        blockStatus = Status.Missing()
                        mapLayer.updateResult(BlockResult.MISS)
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
                    val progress = status.progress

                    drawPrepareBorder(mainColor, progress)
                    scale(INNER_BORDER_SCALE, DefaultCenter) { drawPrepareBorder(mainColor, progress, alpha = INNER_BORDER_ALPHA) }
                    drawSingleNoteFont(rawNoteScale, TextColor, Interpolator.decelerate(progress))
                }
                is Status.Interact -> {
                    drawScaleBlock(mainColor, status.progress)
                    drawFullPrepareBorder(mainColor)
                    scale(INNER_BORDER_SCALE, DefaultCenter) { drawFullPrepareBorder(mainColor, alpha = INNER_BORDER_ALPHA) }
                    drawSingleNoteFont(rawNoteScale, TextColor, 1f)
                }
                is Status.Release -> {
                    val progress = status.progress
                    val releaseProgress = Interpolator.accelerate(1 - progress)
                    val borderAlpha = 3 * progress * (progress - 1) + 1

                    drawBounceBorder(mainColor, 1.875f * progress * (1 - progress) + 1)
                    // drawScaleBlock(mainColor, releaseProgress * status.lastProgress)
                    drawFullPrepareBorder(mainColor, borderAlpha)
                    scale(INNER_BORDER_SCALE, DefaultCenter) { drawFullPrepareBorder(mainColor, alpha = borderAlpha * INNER_BORDER_ALPHA) }
                    drawSingleNoteFont(rawNoteScale, TextColor, releaseProgress)
                    drawLyricsText(TextColor, Interpolator.decelerate(progress) * LYRICS_TEXT_SCALE)
                }
                is Status.Missing -> {
                    val progress = status.progress
                    val missingProgress = Interpolator.accelerate(1 - progress)
                    val missingColor = lerp(mainColor, MissingColor, progress)

                    drawScaleBlock(missingColor, missingProgress)
                    drawFullPrepareBorder(missingColor)
                    scale(INNER_BORDER_SCALE, DefaultCenter) { drawFullPrepareBorder(missingColor, alpha = INNER_BORDER_ALPHA) }
                    drawSingleNoteFont(rawNoteScale, TextColor, missingProgress)
                    drawLyricsText(MissingColor, Interpolator.decelerate(progress) * LYRICS_TEXT_SCALE)
                }
                is Status.Done -> {
                    val borderColor = if (status.isMissing) MissingColor else mainColor

                    drawPrepareBorder(borderColor, 1f)
                    scale(INNER_BORDER_SCALE, DefaultCenter) { drawFullPrepareBorder(borderColor, alpha = INNER_BORDER_ALPHA) }
                    drawLyricsText(if (status.isMissing) MissingColor else TextColor, LYRICS_TEXT_SCALE)
                }
            }
        }
    }
}