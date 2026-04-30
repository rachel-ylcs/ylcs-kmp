package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.fastCoerceAtLeast
import love.yinlin.compose.animation.Interpolator
import love.yinlin.compose.game.character.Character
import love.yinlin.compose.game.character.CharacterLiDiShiGongFenA
import love.yinlin.compose.game.character.CharacterLiDiShiGongFenB
import love.yinlin.compose.game.character.CharacterWuNian
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.common.BlockTime
import love.yinlin.compose.game.common.InteractStatus
import love.yinlin.compose.game.common.InteractTarget
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.rachel.rhyme.RhymeDifficulty

@Stable
class NoteBlock(
    character: Character,
    position: Offset,
    line: BlockLine,
    override val time: Time,
    rawIndex: Int,
    lineIndex: Int,
    override val rhymeAction: RhymeAction.Note,
) : Block<NoteBlock.Status>(character, position, line, rawIndex, lineIndex) {
    @Stable
    data class Time(
        override val appearance: Long,
        val perfectStart: Int,
        val goodStart: Int,
        val badStart: Int,
        val missStart: Int
    ) : BlockTime

    sealed interface Status : BlockStatus {
        class Prepare : Status, BlockStatus.Prepare()
        class Interact : Status, BlockStatus.Interact {
            var progress: Float = 0f
            var result: BlockResult = BlockResult.PERFECT
        }
        class Release(val lastProgress: Float, val result: BlockResult) : Status, BlockStatus.Release() {
            override val duration: Int = 500
        }
        class Missing(val lastProgress: Float) : Status, BlockStatus.Missing() {
            override val duration: Int = 750
        }
        class Done(val isMissing: Boolean, val result: BlockResult) : Status, BlockStatus.Done
    }

    companion object {
        //   PERFECT  ->  GOOD  ->   BAD  ->  MISS
        // 0         0.4        0.7        1
        private const val PERFECT_RANGE = 0.4f
        private const val GOOD_RANGE = 0.7f

        fun buildTime(difficulty: RhymeDifficulty, start: Long, extraPrepareRatio: Float): Time {
            val rawPrepare = PrepareDurationMap[difficulty]!!
            val prepare = (rawPrepare * (1 + extraPrepareRatio)).toInt()
            val interactDuration = rawPrepare / 2
            return Time(
                appearance = start - prepare - PRESS_TOLERANCE,
                perfectStart = prepare,
                goodStart = prepare + (interactDuration * PERFECT_RANGE).toInt(),
                badStart = prepare + (interactDuration * GOOD_RANGE).toInt(),
                missStart = prepare + interactDuration
            )
        }

        private const val INNER_MAIN_ALPHA = 0.5f
    }

    private val rawNoteScale = rhymeAction.scale.toInt()
    private val scaleIndex: Int = (rawNoteScale - 1) % 7 + 1
    private val scaleLevel: Int = (rawNoteScale - 1) / 7
    private val mainColor: Color = ScaleColorList[scaleIndex]

    override val colorList: List<Color> = listOf(mainColor)

    private val isCharacterLiDiShiGongFenA = character is CharacterLiDiShiGongFenA
    private val isCharacterLiDiShiGongFenB = character is CharacterLiDiShiGongFenB

    override fun prepareStatus(): Status = Status.Prepare()

    override fun onInteract(interactStatusList: List<InteractStatus?>, currentStatus: BlockStatus.Interact) {
        val mapLayer = fromMapLayer ?: return
        if (currentStatus !is Status.Interact) return
        // 单击交互只关心按下时刻
        var target: InteractTarget = InteractTarget.None
        for (i in 1 .. 7) {
            val status = interactStatusList[i]
            if (status !is InteractStatus.Down) continue
            // 不存在按下的则标记此轨道按下, 存在按下的则保持多指按下
            target = if (target == InteractTarget.None) InteractTarget.Single(status.id, i) else InteractTarget.Multiple
        }
        // 确定评级结果
        val result = when (val interactTarget = target) {
            is InteractTarget.None -> null // 未按下无事发生
            is InteractTarget.Multiple -> {
                if (character is CharacterWuNian && character.activate()) {
                    // 无视错按
                    mapLayer.backgroundLayer.activateSkill()
                    null
                } else BlockResult.MISS
            } // 多指按下以MISS结算
            is InteractTarget.Single -> {
                if (interactTarget.index == scaleIndex) currentStatus.result
                else if (character is CharacterWuNian && character.activate()) {
                    // 无视错按
                    mapLayer.backgroundLayer.activateSkill()
                    null
                } else BlockResult.MISS
            } // 其他则检查音阶匹配
        } ?: return
        // 处理评级结果
        val lastProgress = currentStatus.progress
        blockStatus = if (result == BlockResult.MISS) Status.Missing(lastProgress) else Status.Release(lastProgress, result)
        mapLayer.updateBlockResult(this, result)
    }

    override fun onUpdate(tick: Int) {
        withMapLayer { mapLayer, audioTick -> // 使用音轨刻
            when (val status = blockStatus) {
                null -> return@withMapLayer false // 未出现不处理
                is Status.Prepare -> updateCustomPrepare(status, audioTick, time.perfectStart, Status::Interact)
                is Status.Interact -> {
                    val progress = ((audioTick - time.perfectStart) / (time.missStart - time.perfectStart).toFloat()).fastCoerceAtLeast(0f)
                    status.progress = progress
                    status.result = when {
                        progress >= 1f -> { // 错过
                            blockStatus = Status.Missing(1f)
                            mapLayer.updateBlockResult(this, BlockResult.MISS) // 提交分数
                            BlockResult.MISS
                        }
                        progress >= GOOD_RANGE -> BlockResult.BAD
                        progress >= PERFECT_RANGE -> BlockResult.GOOD
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

    // 画四角准备框
    private fun Drawer.drawPrepareRectBorder(color: Color, progress: Float) {
        val delta = progress * DEFAULT_RADIUS
        val deltaInv = DEFAULT_DIMENSION - delta

        line(color, TopLeft, Offset(delta, 0f), style = PrepareStroke)
        line(color, TopLeft, Offset(0f, delta), style = PrepareStroke)
        line(color, TopRight, Offset(deltaInv, 0f), style = PrepareStroke)
        line(color, TopRight, Offset(DEFAULT_DIMENSION, delta), style = PrepareStroke)
        line(color, BottomLeft, Offset(0f, deltaInv), style = PrepareStroke)
        line(color, BottomLeft, Offset(delta, DEFAULT_DIMENSION), style = PrepareStroke)
        line(color, BottomRight, Offset(deltaInv, DEFAULT_DIMENSION), style = PrepareStroke)
        line(color, BottomRight, Offset(DEFAULT_DIMENSION, deltaInv), style = PrepareStroke)
    }

    // 画最终态的四角准备框
    private fun Drawer.drawFinalPrepareRectBorder(color: Color, alpha: Float = 1f) {
        rect(color, DefaultRect, alpha = alpha, style = PrepareStroke)
    }

    // 画交互缩放块
    private fun Drawer.drawInteractScaleBlock(color: Color, scaleRatio: Float, alpha: Float) {
        scale(scaleRatio, DefaultCenter) { rect(color, DefaultRect, alpha = alpha) }
    }

    // 画最终态内部块
    private fun Drawer.drawFinalInnerBlock(color: Color, alpha: Float) {
        rect(color, DefaultRect, alpha = alpha)
    }

    override fun Drawer.onDraw() {
        withBlockScale {
            when (val status = blockStatus) {
                null -> return
                is Status.Prepare -> {
                    if (!isCharacterLiDiShiGongFenA) {
                        val progress = status.progress

                        drawPrepareRectBorder(mainColor, progress)
                        drawSingleNoteFont(rawNoteScale, TextColor, Interpolator.decelerate(progress))
                    }
                }
                is Status.Interact -> {
                    if (!isCharacterLiDiShiGongFenA) {
                        drawInteractScaleBlock(mainColor, status.progress, INNER_MAIN_ALPHA)
                        drawFinalPrepareRectBorder(mainColor)
                        drawSingleNoteFont(rawNoteScale, TextColor, 1f)
                    }
                }
                is Status.Release -> {
                    if (!isCharacterLiDiShiGongFenA) {
                        val progress = status.progress
                        val lastProgress = status.lastProgress
                        val releaseProgress = Interpolator.accelerate(1 - progress)

                        if (!isCharacterLiDiShiGongFenB) {
                            withBounceAnimation(progress) {
                                drawBounceAnimation(mainColor) { color, stroke, alpha ->
                                    rect(color, DefaultRect, style = stroke, alpha = alpha)
                                }
                            }
                        }

                        drawInteractScaleBlock(
                            mainColor,
                            Interpolator.map(progress, lastProgress, 1f),
                            Interpolator.map(progress, INNER_MAIN_ALPHA, status.result.alpha)
                        )
                        drawFinalPrepareRectBorder(mainColor, 3 * progress * (progress - 1) + 1)
                        drawSingleNoteFont(rawNoteScale, TextColor, releaseProgress)
                        drawLyricsText(TextColor, (1 - releaseProgress) * LYRICS_TEXT_SCALE)
                    }
                }
                is Status.Missing -> {
                    if (!isCharacterLiDiShiGongFenA) {
                        val progress = status.progress
                        val missingProgress = Interpolator.accelerate(1 - progress)
                        val missingColor = lerp(mainColor, MissingColor, progress)

                        drawInteractScaleBlock(missingColor, missingProgress * status.lastProgress, INNER_MAIN_ALPHA)
                        drawFinalPrepareRectBorder(missingColor)
                        drawSingleNoteFont(rawNoteScale, TextColor, missingProgress)
                        drawLyricsText(MissingColor, (1 - missingProgress) * LYRICS_TEXT_SCALE)
                    }
                }
                is Status.Done -> {
                    if (!status.isMissing) drawFinalInnerBlock(mainColor, status.result.alpha)
                    drawFinalPrepareRectBorder(if (status.isMissing) MissingColor else mainColor)
                    drawLyricsText(if (status.isMissing) MissingColor else TextColor, LYRICS_TEXT_SCALE)
                }
            }
        }
    }
}