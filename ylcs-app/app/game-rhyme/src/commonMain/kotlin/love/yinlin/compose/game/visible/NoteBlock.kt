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
class NoteBlock(
    position: Offset,
    line: BlockLine,
    override val time: Time,
    rawIndex: Int,
    lineIndex: Int,
    override val rhymeAction: RhymeAction.Note,
) : Block<NoteBlock.Status>(position, line, rawIndex, lineIndex) {
    @Stable
    data class Time(
        override val appearance: Long,
        val perfectStart: Int,
        val goodStart: Int,
        val badStart: Int,
        val missStart: Int
    ) : BlockTime

    interface Status : BlockStatus {
        class Prepare : Status, BlockStatus.Prepare {
            var progress: Float = 0f
        }
        class Interact : Status, BlockStatus.Interact {
            var progress: Float = 0f
            var result: BlockResult = BlockResult.PERFECT
        }
        class Release(val lastProgress: Float, val result: BlockResult) : Status, BlockStatus.Release {
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
        fun buildTime(difficulty: RhymeDifficulty, start: Long): Time {
            val prepare = PrepareDurationMap[difficulty]!!
            val interactDuration = prepare / 2
            val perfectDuration = (interactDuration * BlockResult.GOOD.ratio).toInt()
            return Time(
                appearance = start - prepare - perfectDuration / 2,
                perfectStart = prepare,
                goodStart = prepare + perfectDuration,
                badStart = prepare + (interactDuration * BlockResult.BAD.ratio).toInt(),
                missStart = prepare + interactDuration
            )
        }
    }

    private val rawNoteScale = rhymeAction.scale.toInt()
    private val scaleIndex: Int = (rawNoteScale - 1) % 7 + 1
    private val scaleLevel: Int = (rawNoteScale - 1) / 7
    private val mainColor: Color = ScaleColorList[scaleIndex]

    override val colorList: List<Color> = listOf(mainColor)

    override fun prepareStatus(): Status = Status.Prepare()

    override fun onInteract(interactStatus: Array<InteractStatus>, currentStatus: BlockStatus.Interact) {
        if (currentStatus !is Status.Interact) return
        for (i in 0 .. 7) {
            // 单击交互只关心按下时刻
            if (interactStatus[i] == InteractStatus.Down) {
                // 检查按键是否匹配
                val result = if (i == scaleIndex) currentStatus.result else BlockResult.MISS
                blockStatus = if (i == scaleIndex) Status.Release(currentStatus.progress, result) else Status.Missing()
                fromMapLayer?.updateResult(result)
                break
            }
        }
    }

    override fun onUpdate(tick: Int) {
        withMapLayer { mapLayer, audioTick -> // 使用音轨刻
            when (val status = blockStatus) {
                null -> return@withMapLayer false // 未出现不处理
                is Status.Prepare -> {
                    if (audioTick >= time.perfectStart) blockStatus = Status.Interact()
                    else status.progress = (audioTick / time.perfectStart.toFloat()).coerceIn(0f, 1f)
                }
                is Status.Interact -> {
                    val progress = ((audioTick - time.perfectStart) / (time.missStart - time.perfectStart).toFloat()).coerceAtLeast(0f)
                    status.progress = progress
                    status.result = when {
                        progress >= BlockResult.MISS.ratio -> { // 错过
                            blockStatus = Status.Missing()
                            mapLayer.updateResult(BlockResult.MISS) // 提交分数
                            BlockResult.MISS
                        }
                        progress >= BlockResult.BAD.ratio -> BlockResult.BAD
                        progress >= BlockResult.GOOD.ratio -> BlockResult.GOOD
                        else -> BlockResult.PERFECT
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
                    drawSingleNoteFont(rawNoteScale, TextColor, Interpolator.decelerate(progress))
                }
                is Status.Interact -> {
                    drawScaleBlock(mainColor, status.progress)
                    drawFullPrepareBorder(mainColor)
                    drawSingleNoteFont(rawNoteScale, TextColor, 1f)
                }
                is Status.Release -> {
                    val progress = status.progress
                    val releaseProgress = Interpolator.accelerate(1 - progress)

                    drawBounceBorder(mainColor, 1.875f * progress * (1 - progress) + 1)
                    drawScaleBlock(mainColor, releaseProgress * status.lastProgress)
                    drawFullPrepareBorder(mainColor, 3 * progress * (progress - 1) + 1)
                    drawSingleNoteFont(rawNoteScale, TextColor, releaseProgress)
                    drawLyricsText(TextColor, Interpolator.decelerate(progress) * LYRICS_TEXT_SCALE)
                }
                is Status.Missing -> {
                    val progress = status.progress
                    val missingProgress = Interpolator.accelerate(1 - progress)
                    val missingColor = lerp(mainColor, MissingColor, progress)

                    drawScaleBlock(missingColor, missingProgress)
                    drawFullPrepareBorder(missingColor)
                    drawSingleNoteFont(rawNoteScale, TextColor, missingProgress)
                    drawLyricsText(MissingColor, Interpolator.decelerate(progress) * LYRICS_TEXT_SCALE)
                }
                is Status.Done -> {
                    drawPrepareBorder(if (status.isMissing) MissingColor else mainColor, 1f)
                    drawLyricsText(if (status.isMissing) MissingColor else TextColor, LYRICS_TEXT_SCALE)
                }
            }
        }
    }
}