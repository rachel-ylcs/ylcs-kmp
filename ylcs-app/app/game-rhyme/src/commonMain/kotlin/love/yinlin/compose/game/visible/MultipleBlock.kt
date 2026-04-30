package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import love.yinlin.compose.Colors
import love.yinlin.compose.animation.Interpolator
import love.yinlin.compose.extension.Path
import love.yinlin.compose.game.character.Character
import love.yinlin.compose.game.character.CharacterLiDiShiGongFenA
import love.yinlin.compose.game.character.CharacterLiDiShiGongFenB
import love.yinlin.compose.game.character.CharacterSaTuoGe
import love.yinlin.compose.game.character.CharacterWuNian
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.common.BlockTime
import love.yinlin.compose.game.common.InteractStatus
import love.yinlin.compose.game.common.InteractTarget
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.layer.MapLayer
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.rachel.rhyme.RhymeDifficulty

@Stable
class MultipleBlock(
    character: Character,
    position: Offset,
    line: BlockLine,
    override val time: Time,
    rawIndex: Int,
    lineIndex: Int,
    override val rhymeAction: RhymeAction.Slur,
) : Block<MultipleBlock.Status>(character, position, line, rawIndex, lineIndex) {
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
        class Done(
            val notePressedList: List<Boolean>,
            val isMissing: Boolean,
            val result: BlockResult
        ) : Status, BlockStatus.Done
    }

    companion object {
        private const val INTERACT_NEW_NOTE_DURATION = 200 // 新音符添加动画时长

        private const val INNER_TIP_ALPHA = 0.3f
        private const val INNER_MAIN_ALPHA = 0.8f
        private val InnerProgressTipColor = Colors.White

        private val DefaultPath = Path(arrayOf(TopCenter, CenterRight, BottomCenter, CenterLeft))
        private val InnerPath2 = listOf(
            Path(arrayOf(TopCenter, CenterLeft, CenterRight)),
            Path(arrayOf(BottomCenter, CenterLeft, CenterRight))
        )
        private val InnerPath3 = listOf(
            Path(arrayOf(CenterLeft, InnerTopLeft, InnerBottomLeft)),
            Path(arrayOf(TopCenter, InnerTopLeft, InnerBottomLeft, BottomCenter, InnerBottomRight, InnerTopRight)),
            Path(arrayOf(CenterRight, InnerTopRight, InnerBottomRight))
        )
        private val InnerPath4 = listOf(
            Path(arrayOf(TopCenter, DefaultCenter, CenterLeft)),
            Path(arrayOf(TopCenter, DefaultCenter, CenterRight)),
            Path(arrayOf(BottomCenter, DefaultCenter, CenterRight)),
            Path(arrayOf(BottomCenter, DefaultCenter, CenterLeft))
        )
        private val InnerPaths = listOf(emptyList(), emptyList(), InnerPath2, InnerPath3, InnerPath4)

        fun buildTime(difficulty: RhymeDifficulty, start: Long, end: Long, extraPrepareRatio: Float): Time {
            val rawPrepare = PrepareDurationMap[difficulty]!!
            val prepare = (rawPrepare * (1 + extraPrepareRatio)).toInt()
            val duration = maxOf((end - start).toInt(), rawPrepare / 2)
            return Time(
                appearance = start - prepare - PRESS_TOLERANCE,
                start = prepare,
                end = prepare + duration
            )
        }
    }

    private val noteCount: Int = rhymeAction.scale.size
    private val rawNoteScaleList: List<Int> = rhymeAction.scale.fastMap { it.toInt() }
    internal val scaleIndexs: List<Int> = rawNoteScaleList.fastMap { (it - 1) % 7 + 1 }
    private val scaleLevels: List<Int> = rawNoteScaleList.fastMap { (it - 1) / 7 }

    override val colorList: List<Color> = scaleIndexs.fastMap { ScaleColorList[it] }

    private val isCharacterLiDiShiGongFenA = character is CharacterLiDiShiGongFenA
    private val isCharacterLiDiShiGongFenB = character is CharacterLiDiShiGongFenB
    private val isCharacterSaTuoGe = character is CharacterSaTuoGe

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
        updateBlockResult(this@MultipleBlock, result)
    }

    override fun onInteract(interactStatusList: List<InteractStatus?>, currentStatus: BlockStatus.Interact) {
        val mapLayer = fromMapLayer ?: return
        if (currentStatus !is Status.Interact) return
        var target: InteractTarget = InteractTarget.None
        for (i in 1 .. 7) {
            val status = interactStatusList[i]
            if (status !is InteractStatus.Down) continue
            target = if (target == InteractTarget.None) InteractTarget.Single(status.id, i) else InteractTarget.Multiple
        }
        // 确定评级结果
        when (val interactTarget = target) {
            is InteractTarget.None -> { } // 未按下无事发生
            is InteractTarget.Multiple -> {
                if (character is CharacterWuNian && character.activate()) mapLayer.backgroundLayer.activateSkill()
                else mapLayer.updateCustomResult(currentStatus.progress, currentStatus.noteProgressList) // 多指立即结算
            } // 多指按下立即结算
            is InteractTarget.Single -> { // 单指按下
                if (isCharacterSaTuoGe) {
                    var isConsumed = false
                    for (i in 0 ..< noteCount) {
                        val p = currentStatus.noteProgressList[i]
                        if (p != null) continue // 已经按过
                        if (interactTarget.index == scaleIndexs[i]) { // 匹配
                            isConsumed = true
                            currentStatus.noteProgressList[i] = 0f // 设置动画进度
                            // 检查是否已经全部完成, 立即结算
                            if (currentStatus.noteProgressList.fastAll { it != null }) mapLayer.updateCustomResult(currentStatus.progress, currentStatus.noteProgressList)
                            break
                        }
                        // 不匹配无视顺序继续找
                    }
                    // 没找到立即结算
                    if (!isConsumed) mapLayer.updateCustomResult(currentStatus.progress, currentStatus.noteProgressList)
                }
                else {
                    for (i in 0 ..< noteCount) {
                        val p = currentStatus.noteProgressList[i]
                        if (p != null) continue // 已经达到此阶段
                        // 检查对应索引的音符是否匹配
                        if (interactTarget.index == scaleIndexs[i]) { // 匹配
                            currentStatus.noteProgressList[i] = 0f // 设置动画进度
                            // 检查是否已经是最后一个阶段, 立即结算
                            if (i == noteCount - 1) mapLayer.updateCustomResult(currentStatus.progress, currentStatus.noteProgressList)
                        }
                        else if (character is CharacterWuNian && character.activate()) mapLayer.backgroundLayer.activateSkill() // 无视错按
                        else mapLayer.updateCustomResult(currentStatus.progress, currentStatus.noteProgressList) // 立即结算
                        break
                    }
                    // 理论上不可达
                }
            }
        }
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
                        if (noteProgress != null) { // 更新阶段进度
                            status.noteProgressList[index] = (noteProgress + tick / INTERACT_NEW_NOTE_DURATION.toFloat()).fastCoerceAtMost(1f)
                        }
                    }
                    if (progress >= 1f) mapLayer.updateCustomResult(1f, status.noteProgressList)
                }
                is Status.Release -> updateCustomRelease(status, tick) { Status.Done(status.noteProgressList.map { p -> p != null },false, it.result) }
                is Status.Missing -> updateCustomRelease(status, tick) { Status.Done(status.noteProgressList.map { p -> p != null },true, BlockResult.MISS) }
                is Status.Done -> return@withMapLayer false
            }
            true
        }
    }

    private val colorListProvider = { index: Int -> colorList[index] }
    private val missingColorProvider = { _: Int -> MissingColor }

    private val drawPrepareDiamondBorder: Drawer.(Float) -> Unit = when (noteCount) {
        2 -> { p -> drawPrepareDiamondBorder2(p) }
        3 -> { p -> drawPrepareDiamondBorder3(p) }
        else -> { p -> drawPrepareDiamondBorder4(p) }
    }

    // 画菱形准备框
    private fun Drawer.drawPrepareDiamondBorder2(progress: Float) {
        val delta = progress * DEFAULT_RADIUS
        val deltaInv = DEFAULT_RADIUS - delta
        val color1 = colorList[0]
        val color2 = colorList[1]

        line(color1, TopCenter, Offset(deltaInv, delta), style = PrepareStroke)
        line(color1, TopCenter, Offset(DEFAULT_DIMENSION - deltaInv, delta), style = PrepareStroke)
        line(color2, BottomCenter, Offset(deltaInv, DEFAULT_DIMENSION - delta), style = PrepareStroke)
        line(color2, BottomCenter, Offset(DEFAULT_DIMENSION - deltaInv, DEFAULT_DIMENSION - delta), style = PrepareStroke)
    }

    private fun Drawer.drawPrepareDiamondBorder3(progress: Float) {
        val delta = progress * DEFAULT_RADIUS / 2
        val negDelta = DEFAULT_RADIUS - delta
        val posDelta = DEFAULT_RADIUS + delta
        val negDeltaTotal = DEFAULT_DIMENSION - delta

        val color1 = colorList[0]
        val color2 = colorList[1]
        val color3 = colorList[2]

        line(color1, CenterLeft, Offset(delta, negDelta), style = PrepareStroke)
        line(color1, CenterLeft, Offset(delta, posDelta), style = PrepareStroke)
        line(color2, TopCenter, Offset(negDelta, delta), style = PrepareStroke)
        line(color2, TopCenter, Offset(posDelta, delta), style = PrepareStroke)
        line(color2, BottomCenter, Offset(negDelta, negDeltaTotal), style = PrepareStroke)
        line(color2, BottomCenter, Offset(posDelta, negDeltaTotal), style = PrepareStroke)
        line(color3, CenterRight, Offset(negDeltaTotal, negDelta), style = PrepareStroke)
        line(color3, CenterRight, Offset(negDeltaTotal, posDelta), style = PrepareStroke)
    }

    private fun Drawer.drawPrepareDiamondBorder4(progress: Float) {
        val delta = progress * DEFAULT_RADIUS
        val negDelta = DEFAULT_RADIUS - delta
        val posDelta = DEFAULT_RADIUS + delta
        val negDeltaTotal = DEFAULT_DIMENSION - delta

        line(colorList[0], CenterLeft, Offset(delta, negDelta), style = PrepareStroke)
        line(colorList[1], TopCenter, Offset(posDelta, delta), style = PrepareStroke)
        line(colorList[2], CenterRight, Offset(negDeltaTotal, posDelta), style = PrepareStroke)
        line(colorList[3], BottomCenter, Offset(negDelta, negDeltaTotal), style = PrepareStroke)
    }

    private val drawFinalPrepareDiamondBorder: Drawer.((Int) -> Color, Float) -> Unit = when (noteCount) {
        2 -> { c, p -> drawFinalPrepareDiamondBorder2(c, p) }
        3 -> { c, p -> drawFinalPrepareDiamondBorder3(c, p) }
        else -> { c, p -> drawFinalPrepareDiamondBorder4(c, p) }
    }

    // 画最终态菱形准备框
    private fun Drawer.drawFinalPrepareDiamondBorder2(colorProvider: (Int) -> Color, alpha: Float) {
        val color1 = colorProvider(0)
        val color2 = colorProvider(1)

        line(color1, TopCenter, CenterLeft, style = PrepareStroke, alpha = alpha)
        line(color1, TopCenter, CenterRight, style = PrepareStroke, alpha = alpha)
        line(color2, BottomCenter, CenterLeft, style = PrepareStroke, alpha = alpha)
        line(color2, BottomCenter, CenterRight, style = PrepareStroke, alpha = alpha)
    }

    private fun Drawer.drawFinalPrepareDiamondBorder3(colorProvider: (Int) -> Color, alpha: Float) {
        val color1 = colorProvider(0)
        val color2 = colorProvider(1)
        val color3 = colorProvider(2)

        line(color1, CenterLeft, InnerTopLeft, style = PrepareStroke, alpha = alpha)
        line(color1, CenterLeft, InnerBottomLeft, style = PrepareStroke, alpha = alpha)
        line(color2, TopCenter, InnerTopLeft, style = PrepareStroke, alpha = alpha)
        line(color2, TopCenter, InnerTopRight, style = PrepareStroke, alpha = alpha)
        line(color2, BottomCenter, InnerBottomLeft, style = PrepareStroke, alpha = alpha)
        line(color2, BottomCenter, InnerBottomRight, style = PrepareStroke, alpha = alpha)
        line(color3, CenterRight, InnerTopRight, style = PrepareStroke, alpha = alpha)
        line(color3, CenterRight, InnerBottomRight, style = PrepareStroke, alpha = alpha)
    }

    private fun Drawer.drawFinalPrepareDiamondBorder4(colorProvider: (Int) -> Color, alpha: Float) {
        line(colorProvider(0), CenterLeft, TopCenter, style = PrepareStroke, alpha = alpha)
        line(colorProvider(1), TopCenter, CenterRight, style = PrepareStroke, alpha = alpha)
        line(colorProvider(2), CenterRight, BottomCenter, style = PrepareStroke, alpha = alpha)
        line(colorProvider(3), BottomCenter, CenterLeft, style = PrepareStroke, alpha = alpha)
    }

    private val drawBounceDiamondBorder: Drawer.(Float) -> Unit = when (noteCount) {
        2 -> { p -> drawBounceDiamondBorder2(p) }
        3 -> { p -> drawBounceDiamondBorder3(p) }
        else -> { p -> drawBounceDiamondBorder4(p) }
    }

    private fun Drawer.drawBounceDiamondBorder2(progress: Float) {
        withBounceAnimation(progress) {
            drawBounceAnimation(colorList[0]) { color, stroke, alpha ->
                line(color, TopCenter, CenterLeft, style = stroke, alpha = alpha)
                line(color, TopCenter, CenterRight, style = stroke, alpha = alpha)
            }
            drawBounceAnimation(colorList[1]) { color, stroke, alpha ->
                line(color, BottomCenter, CenterLeft, style = stroke, alpha = alpha)
                line(color, BottomCenter, CenterRight, style = stroke, alpha = alpha)
            }
        }
    }

    private fun Drawer.drawBounceDiamondBorder3(progress: Float) {
        withBounceAnimation(progress) {
            drawBounceAnimation(colorList[0]) { color, stroke, alpha ->
                line(color, CenterLeft, InnerTopLeft, style = stroke, alpha = alpha)
                line(color, CenterLeft, InnerBottomLeft, style = stroke, alpha = alpha)
            }
            drawBounceAnimation(colorList[1]) { color, stroke, alpha ->
                line(color, TopCenter, InnerTopLeft, style = stroke, alpha = alpha)
                line(color, TopCenter, InnerTopRight, style = stroke, alpha = alpha)
                line(color, BottomCenter, InnerBottomLeft, style = stroke, alpha = alpha)
                line(color, BottomCenter, InnerBottomRight, style = stroke, alpha = alpha)
            }
            drawBounceAnimation(colorList[2]) { color, stroke, alpha ->
                line(color, CenterRight, InnerTopRight, style = stroke, alpha = alpha)
                line(color, CenterRight, InnerBottomRight, style = stroke, alpha = alpha)
            }
        }
    }

    private fun Drawer.drawBounceDiamondBorder4(progress: Float) {
        withBounceAnimation(progress) {
            drawBounceAnimation(colorList[0]) { color, stroke, alpha ->
                line(color, CenterLeft, TopCenter, style = stroke, alpha = alpha)
            }
            drawBounceAnimation(colorList[1]) { color, stroke, alpha ->
                line(color, TopCenter, CenterRight, style = stroke, alpha = alpha)
            }
            drawBounceAnimation(colorList[2]) { color, stroke, alpha ->
                line(color, CenterRight, BottomCenter, style = stroke, alpha = alpha)
            }
            drawBounceAnimation(colorList[3]) { color, stroke, alpha ->
                line(color, BottomCenter, CenterLeft, style = stroke, alpha = alpha)
            }
        }
    }

    // 画提示框
    private fun Drawer.drawInteractScaleBlock(color: Color, scaleRatio: Float, alpha: Float) {
        scale(scaleRatio, DefaultCenter) {
            path(color, DefaultPath, alpha = alpha)
        }
    }

    // 画已交互状态
    private fun Drawer.drawInteractPressedArea(progressList: List<Float?>, alpha: Float) {
        repeat(noteCount) { index ->
            progressList[index]?.let { progress ->
                path(colorList[index], InnerPaths[noteCount][index], alpha = alpha * progress)
            }
        }
    }

    private fun Drawer.drawInteractPressedArea(p: Float, progressList: List<Float?>, alpha: Float) {
        repeat(noteCount) { index ->
            progressList[index]?.let { progress ->
                path(colorList[index], InnerPaths[noteCount][index], alpha = Interpolator.map(p, progress, alpha))
            }
        }
    }

    private fun Drawer.drawFinalPressedArea(notePressedList: List<Boolean>, alpha: Float) {
        repeat(noteCount) { index ->
            if (notePressedList[index]) {
                path(colorList[index], InnerPaths[noteCount][index], alpha = alpha)
            }
        }
    }

    override fun Drawer.onDraw() {
        withBlockScale {
            when (val status = blockStatus) {
                null -> return
                is Status.Prepare -> {
                    if (!isCharacterLiDiShiGongFenA) {
                        val progress = status.progress

                        drawPrepareDiamondBorder(progress)
                        drawMultipleNoteFont(rawNoteScaleList, TextColor, Interpolator.decelerate(progress))
                    }
                }
                is Status.Interact -> {
                    if (!isCharacterLiDiShiGongFenA) {
                        drawInteractScaleBlock(InnerProgressTipColor, status.progress, INNER_TIP_ALPHA)
                        drawInteractPressedArea(status.noteProgressList, INNER_MAIN_ALPHA)
                        drawFinalPrepareDiamondBorder(colorListProvider, 1f)
                        drawMultipleNoteFont(rawNoteScaleList, TextColor, 1f)
                    }
                }
                is Status.Release -> {
                    if (!isCharacterLiDiShiGongFenA) {
                        val progress = status.progress
                        val releaseProgress = Interpolator.accelerate(1 - progress)

                        if (!isCharacterLiDiShiGongFenB) {
                            drawBounceDiamondBorder(progress)
                        }

                        drawInteractScaleBlock(InnerProgressTipColor, status.lastProgress, INNER_TIP_ALPHA * releaseProgress)
                        drawInteractPressedArea(progress, status.noteProgressList, INNER_MAIN_ALPHA)
                        drawFinalPrepareDiamondBorder(colorListProvider, 3 * progress * (progress - 1) + 1)
                        drawMultipleNoteFont(rawNoteScaleList, TextColor, releaseProgress)
                        drawLyricsText(TextColor, (1 - releaseProgress) * LYRICS_TEXT_SCALE)
                    }
                }
                is Status.Missing -> {
                    if (!isCharacterLiDiShiGongFenA) {
                        val progress = status.progress
                        val missingProgress = Interpolator.accelerate(1 - progress)

                        drawInteractScaleBlock(InnerProgressTipColor, status.lastProgress, INNER_TIP_ALPHA * missingProgress)
                        drawInteractPressedArea(progress, status.noteProgressList, 0f)
                        drawFinalPrepareDiamondBorder({ lerp(colorList[it], MissingColor, progress) }, 1f)
                        drawMultipleNoteFont(rawNoteScaleList, TextColor, missingProgress)
                        drawLyricsText(MissingColor, (1 - missingProgress) * LYRICS_TEXT_SCALE)
                    }
                }
                is Status.Done -> {
                    if (status.isMissing) {
                        drawFinalPrepareDiamondBorder(missingColorProvider, 1f)
                        drawLyricsText(MissingColor, LYRICS_TEXT_SCALE)
                    }
                    else {
                        drawFinalPressedArea(status.notePressedList, INNER_MAIN_ALPHA)
                        drawFinalPrepareDiamondBorder(colorListProvider, 1f)
                        drawLyricsText(TextColor, LYRICS_TEXT_SCALE)
                    }
                }
            }
        }
    }
}