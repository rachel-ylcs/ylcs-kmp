package love.yinlin.common.rhyme.data

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.animation.LineFrameAnimation
import love.yinlin.compose.game.animation.SpeedAdapter
import love.yinlin.data.music.RhymeAction
import love.yinlin.common.rhyme.RhymeAssets
import love.yinlin.common.rhyme.RhymePlayConfig

@Stable
class OffsetSlurAction(
    assets: RhymeAssets,
    playConfig: RhymePlayConfig,
    start: Long,
    end: Long,
    override val action: RhymeAction.Slur
) : DynamicAction(assets, playConfig, start, end) {
    @Stable
    private interface BasicMultiProgress {
        val size: Int
        var head: Float
        val valid: Boolean
        val snapshot: List<Float>
        fun foreachTrailing(block: (Int, Float, Float) -> Unit)
    }

    @Stable
    private class FixedMultiProgress(
        private val duration: Long,
        actionDuration: Long,
        override val size: Int
    ) : BasicMultiProgress {
        private val perActionDuration = actionDuration / size

        override var head: Float by mutableFloatStateOf(0f)

        // 尾部计算公式 tail = A[(V[head] * D - (index + 1) * PAD) / D]
        private fun calcTailProgress(index: Int): Float = ((head.asUncheckedVirtual * duration - (index + 1) * perActionDuration) / duration).asUncheckedActual

        override val valid: Boolean get() = head > calcTailProgress(0)

        override val snapshot: List<Float> get() = buildList {
            add(head)
            for (index in 0 ..< this@FixedMultiProgress.size) add(calcTailProgress(index))
        }

        override fun foreachTrailing(block: (Int, Float, Float) -> Unit) {
            var headProgress = head
            for (index in 0 ..< size) {
                val tailProgress = calcTailProgress(index)
                block(index, headProgress, tailProgress)
                headProgress = tailProgress
            }
        }
    }

    @Stable
    private class MultiProgress(initProgress: List<Float>) : BasicMultiProgress {
        override val size: Int = initProgress.size - 1

        private val items = initProgress.map { mutableFloatStateOf(it) }
        var currentIndex by mutableIntStateOf(0)

        override var head: Float by items[0]

        override val valid: Boolean get() = items[0].value > items[currentIndex + 1].value

        override val snapshot: List<Float> get() = items.map { it.value }

        val currentHeadProgress: Float get() = items[currentIndex].value
        val currentTailProgress: Float get() = items[currentIndex + 1].value

        fun transferTrack(): Boolean {
            if (currentIndex < size - 1) {
                ++currentIndex
                return false
            }
            return true
        }

        inline fun updateTail(block: (Int, Float) -> Float) {
            for (index in currentIndex ..< size) {
                items[index + 1].value = block(index, items[index + 1].value)
            }
        }

        override fun foreachTrailing(block: (Int, Float, Float) -> Unit) {
            var headProgress = items[currentIndex].value
            for (index in currentIndex ..< size) {
                val tailProgress = items[index + 1].value
                block(index, headProgress, tailProgress)
                headProgress = tailProgress
            }
        }
    }

    @Stable
    private sealed interface State {
        @Stable
        data object Ready : State // 就绪
        @Stable
        class Moving(duration: Long, actionDuration: Long, actionSize: Int) : State { // 移动
            val progress = FixedMultiProgress(duration, actionDuration, actionSize)
        }
        @Stable
        class Pressing(
            frameCount: Int,
            val result: ActionResult,
            val initTrackIndex: Int,
            lastProgress: FixedMultiProgress,
        ) : State { // 长按中
            var currentTrackIndex by mutableIntStateOf(initTrackIndex)
            val progress = MultiProgress(lastProgress.snapshot)
            val animation = LineFrameAnimation(frameCount, true).also { it.start() }
        }
        @Stable
        class Releasing(
            frameCount: Int,
            val result: ActionResult,
            val lastProgress: MultiProgress,
        ) : State { // 释放中
            val animation = LineFrameAnimation(frameCount).also { it.start() }
        }
        @Stable
        class Missing(frameCount: Int, lastProgress: FixedMultiProgress) : State { // 错过中
            val progress = MultiProgress(lastProgress.snapshot)
            val animation = LineFrameAnimation(frameCount / 2, adapter = SpeedAdapter(0.5f)).also { it.start() }
        }
        @Stable
        data object Done : State // 已完成
    }

    @Stable
    private data class ActionTick(var press: Long? = null, var release: Long? = null)

    @Stable
    private class ActionTicks(size: Int) {
        private val items: List<ActionTick> = List(size) { ActionTick() }

        fun press(index: Int, tick: Long) {
            val item = items[index]
            if (item.press == null) item.press = tick
        }

        fun release(index: Int, tick: Long) {
            val item = items[index]
            if (item.press != null && item.release == null) item.release = tick
        }

        operator fun iterator() = items.iterator()
    }

    private val actionSize = action.scale.size
    private val trackIndexs = action.scale.map { mapTrackIndex(it) }
    private val noteLevels = action.scale.map { mapNoteLevel(it) }
    private val blockRects = noteLevels.map { calcBlockRect(it + 3) }

    private var state: State by mutableStateOf(State.Ready) // 状态

    private val actionTicks = ActionTicks(actionSize)

    override var bindId: Long? = null

    override val appearance: Long = start - (baseDuration * HIT_RATIO.asUncheckedVirtual).toLong()
    private val actionDuration = end - start // 长按总时长
    private val perActionDuration = actionDuration / actionSize // 每个音符长按时长
    private val tailAppearances = List(actionSize) { index ->
        appearance + (index + 1) * perActionDuration
    }

    override val isDismiss: Boolean by derivedStateOf { state == State.Done }

    override fun onAdmission() { state = State.Moving(baseDuration, actionDuration, actionSize) }

    private fun ActionCallback.calcResult(result: ActionResult) {
        // 计算长按时间
        var coefficient = 0f
        for ((pressTick, releaseTick) in actionTicks) {
            if (pressTick != null && releaseTick != null) {
                val pressDuration = (releaseTick - pressTick).coerceAtLeast(0L)
                val ratio = pressDuration / perActionDuration.toFloat()
                coefficient += ratio * ratio * (ratio + 1)
            }
        }
        val perCoefficient = (1f + coefficient / actionSize).coerceIn(1f, 2f)
        updateResult(result, perCoefficient)
    }

    override fun onUpdate(tick: Long, callback: ActionCallback) {
        when (val currentState = state) {
            State.Ready, State.Done -> { } // 就绪或完成状态不更新
            is State.Moving -> {
                // 更新进度
                val progress = currentState.progress
                val headProgress = ((tick - appearance) / baseDurationF).asActual
                progress.head = headProgress
                // 超出死线仍未处理的音符标记错过
                if (headProgress > deadline) {
                    state = State.Missing(noteDismiss.frameCount, progress)
                    callback.updateResult(ActionResult.MISS)
                }
            }
            is State.Pressing -> {
                val progress = currentState.progress
                currentState.animation.update()
                progress.updateTail { index, _ ->
                    ((tick - tailAppearances[index]) / baseDurationF).asUncheckedActual.coerceAtMost(0.95f)
                }
                // 尾部到达头部时变轨
                val currentHeadProgress = progress.currentHeadProgress
                val currentTailProgress = progress.currentTailProgress
                if (currentTailProgress > currentHeadProgress) {
                    // 记录释放时间
                    actionTicks.release(progress.currentIndex, tick)
                    // 所有轨道都完成时自动结算
                    if (progress.transferTrack()) {
                        state = State.Releasing(
                            longRelease.frameCount,
                            currentState.result,
                            progress
                        ).also {
                            callback.calcResult(it.result)
                        }
                    }
                }
            }
            is State.Releasing -> {
                if (!currentState.animation.update()) state = State.Done
            }
            is State.Missing -> {
                val progress = currentState.progress
                if (!currentState.animation.update()) state = State.Done
                progress.head = ((tick - appearance) / baseDurationF).asUncheckedActual.coerceAtMost(0.95f)
                progress.updateTail { index, _ ->
                    ((tick - tailAppearances[index]) / baseDurationF).asUncheckedActual.coerceAtMost(0.95f)
                }
            }
        }
    }

    override fun onTrackDown(track: Track, tick: Long, callback: ActionCallback): Boolean {
        // 校验轨道一致, 状态移动中
        val currentState = state
        val initTrackIndex = trackIndexs[0]
        if (track.index != initTrackIndex || currentState !is State.Moving) return false
        val progress = currentState.progress
        return ActionResult.inRange(HIT_RATIO, progress.head)?.also { result ->
            state = if (result == ActionResult.MISS) { // 错过
                callback.updateResult(result)
                State.Missing(noteDismiss.frameCount, progress)
            } else {
                actionTicks.press(0, tick)
                State.Pressing(
                    longPress.frameCount,
                    result = result,
                    initTrackIndex = initTrackIndex,
                    lastProgress = progress
                )
            }
        } != null
    }

    override fun onTrackUp(track: Track, tick: Long, callback: ActionCallback) {
        val currentState = state
        if (currentState is State.Pressing) {
            val progress = currentState.progress
            actionTicks.release(progress.currentIndex, tick)
            state = State.Releasing(
                longRelease.frameCount,
                currentState.result,
                progress
            ).also {
                callback.calcResult(it.result)
            }
        }
    }

    override fun onTrackTransfer(oldTrack: Track, newTrack: Track, tick: Long, callback: ActionCallback): Boolean {
        val currentState = state
        if (currentState !is State.Pressing) return false
        val progress = currentState.progress
        val currentProgressIndex = progress.currentIndex
        val nextProgressIndex = currentProgressIndex + 1
        val currentTrackIndex = trackIndexs[currentProgressIndex]
        val nextTrackIndex = trackIndexs.getOrNull(nextProgressIndex)
        val oldTrackIndex = oldTrack.index
        val newTrackIndex = newTrack.index
        currentState.currentTrackIndex = newTrackIndex
        // 检查从当前轨道移出
        if (oldTrackIndex == currentTrackIndex) {
            actionTicks.release(currentProgressIndex, tick)
            currentState.progress.transferTrack()
        }
        // 移动到新轨道
        if (newTrackIndex == nextTrackIndex) actionTicks.press(nextProgressIndex, tick)
        return true
    }

    private fun Drawer.drawMultiTrailing(progress: BasicMultiProgress, alpha: Float = 1f) {
        if (!progress.valid || alpha <= 0f) return
        // 绘制拖尾
        var terminalResult: Pair<Offset, Offset>? = null
        progress.foreachTrailing { index, headProgress, tailProgress ->
            terminalResult = drawTrailing(Tracks[trackIndexs[index]], noteLevels[index], headProgress, tailProgress, alpha, terminalResult)
        }
    }

    override fun Drawer.onDraw() {
        val currentState = state
        if (currentState == State.Ready || currentState == State.Done) return // 就绪或完成状态不渲染

        when (currentState) {
            is State.Moving -> {
                val progress = currentState.progress
                val track = Tracks[trackIndexs[0]]
                val blockRect = blockRects[0]
                val imageRect = if (track.isCenter) blockRect.second else blockRect.first
                // 拖尾
                drawMultiTrailing(progress, 1f)
                // 按键
                noteTransform(progress.head, track) {
                    image(blockMap, imageRect, it)
                }
            }
            is State.Pressing -> {
                val progress = currentState.progress
                // 拖尾
                drawMultiTrailing(progress, 1f)
                // 动画
                drawPlainAnimation(Tracks[currentState.currentTrackIndex], HIT_RATIO, longPress, currentState.animation, scaleRatio = 1.25f, colorFilter = ResultColorFilters[currentState.result.ordinal])
            }
            is State.Releasing -> {
                val progress = currentState.lastProgress
                val releasingAlpha = (1 - currentState.animation.progress * 1.5f).coerceAtLeast(0f)
                val track = Tracks[trackIndexs[progress.currentIndex]]
                // 拖尾
                drawMultiTrailing(progress, releasingAlpha)
                // 动画
                drawPlainAnimation(track, HIT_RATIO, longRelease, currentState.animation, scaleRatio = 1.25f, colorFilter = ResultColorFilters[currentState.result.ordinal])
            }
            is State.Missing -> {
                val progress = currentState.progress
                val track = Tracks[trackIndexs[0]]
                val blockRect = blockRects[0]
                val imageRect = if (track.isCenter) blockRect.second else blockRect.first
                val missAlpha = (1 - currentState.animation.progress * 2f).coerceAtLeast(0f)
                // 拖尾
                drawMultiTrailing(progress, missAlpha)
                // 按键
                noteTransform(progress.head, track) {
                    if (missAlpha > 0f) image(blockMap, imageRect, it, alpha = missAlpha)
                    drawPerspectiveAnimation(track, it, noteDismiss, currentState.animation, colorFilter = SlurColorFilters[noteLevels[0]])
                }
            }
        }
    }
}