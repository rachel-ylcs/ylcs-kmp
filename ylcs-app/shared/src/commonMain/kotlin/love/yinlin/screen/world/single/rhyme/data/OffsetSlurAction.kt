package love.yinlin.screen.world.single.rhyme.data

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastForEachIndexed
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.animation.LineFrameAnimation
import love.yinlin.compose.game.animation.SpeedAdapter
import love.yinlin.data.music.RhymeAction
import love.yinlin.screen.world.single.rhyme.RhymeAssets
import love.yinlin.screen.world.single.rhyme.RhymePlayConfig

@Stable
class OffsetSlurAction(
    assets: RhymeAssets,
    playConfig: RhymePlayConfig,
    start: Long,
    end: Long,
    override val action: RhymeAction.Slur
) : DynamicAction(assets, playConfig, start, end) {
    @Stable
    private class MultiProgress (initValue: List<Float>) {
        constructor(size: Int) : this(List(size) { 0f })
        private val items = initValue.map { mutableFloatStateOf(it) }
        private val size = items.size
        operator fun get(index: Int) = items[index].value
        operator fun set(index: Int, value: Float) { items[index].value = value }
    }

    @Stable
    private sealed interface State {
        @Stable
        data object Ready : State // 就绪
        @Stable
        class Moving(
            private val duration: Long,
            private val actionSize: Int,
            private val perActionDuration: Long,
        ) : State { // 移动
            // 头部进度
            var headProgress by mutableFloatStateOf(0f)
            // 尾部计算哦公式 tail = A[(V[head] * D - (index + 1) * PAD) / D]
            fun tailProgress(index: Int): Float = ((headProgress.asUncheckedVirtual * duration - (index + 1) * perActionDuration) / duration).asUncheckedActual
            val tailProgressList: List<Float> get() = List(actionSize) { tailProgress(it) }
        }
        @Stable
        class Pressing : State { // 长按中

        }
        @Stable
        class Releasing : State { // 释放中

        }
        @Stable
        class Missing(
            frameCount: Int,
            lastHeadProgress: Float,
            lastTailProgressList: List<Float>,
        ) : State { // 错过中
            var headProgress by mutableFloatStateOf(lastHeadProgress)
            var tailProgressList = MultiProgress(lastTailProgressList)
            val animation = LineFrameAnimation(frameCount / 2, adapter = SpeedAdapter(0.5f)).also { it.start() }
        }
        @Stable
        data object Done : State // 已完成
    }

    private val actionSize = action.scale.size
    private val trackIndexs = action.scale.map { mapTrackIndex(it) }
    private val noteScales = action.scale.map { mapNoteScale(it) }
    private val blockRects = noteScales.map { calcBlockRect(it + 3) }

    private var state: State by mutableStateOf(State.Ready) // 状态

    override var bindId: Long? = null

    override val appearance: Long = start - (baseDuration * HIT_RATIO.asUncheckedVirtual).toLong()
    private val actionDuration = end - start // 长按总时长
    private val perActionDuration = actionDuration / actionSize // 每个音符长按时长
    private val tailAppearances = List(actionSize) { index ->
        appearance + (index + 1) * perActionDuration
    }

    override val isDismiss: Boolean by derivedStateOf { state == State.Done }

    override fun onAdmission() { state = State.Moving(baseDuration, actionSize, perActionDuration) }

    private fun ActionCallback.calcResult(state: State.Releasing) {

    }

    override fun onUpdate(tick: Long, callback: ActionCallback) {
        when (val currentState = state) {
            State.Ready, State.Done -> { } // 就绪或完成状态不更新
            is State.Moving -> {
                // 更新进度
                val headProgress = ((tick - appearance) / baseDurationF).asActual
                currentState.headProgress = headProgress
                // 超出死线仍未处理的音符标记错过
                if (headProgress > deadline) {
                    state = State.Missing(noteDismiss.frameCount, headProgress, currentState.tailProgressList)
                    callback.updateResult(ActionResult.MISS)
                }
            }
            is State.Pressing -> {

            }
            is State.Releasing -> {

            }
            is State.Missing -> {
                if (!currentState.animation.update()) state = State.Done
                currentState.headProgress = ((tick - appearance) / baseDurationF).asUncheckedActual.coerceAtMost(0.95f)
                for (index in 0 ..< actionSize) {
                    currentState.tailProgressList[index] = ((tick - tailAppearances[index]) / baseDurationF).asUncheckedActual.coerceAtMost(0.95f)
                }
            }
        }
    }

    override fun onTrackDown(track: Track, tick: Long, callback: ActionCallback): Boolean {
        return false
    }

    override fun onTrackUp(track: Track, tick: Long, callback: ActionCallback) {

    }

    override fun onTrackTransfer(oldTrack: Track, newTrack: Track, tick: Long, callback: ActionCallback): Boolean {
        return false
    }

    private fun Drawer.drawTrailing(headProgress: Float, tailIndex: Int, tailProgressList: List<Float>, alpha: Float = 1f) {
        if (headProgress <= tailProgressList[tailIndex] || alpha <= 0f) return
        // 绘制拖尾

    }

    override fun Drawer.onDraw() {
        val currentState = state
        if (currentState == State.Ready || currentState == State.Done) return // 就绪或完成状态不渲染

        when (currentState) {
            is State.Moving -> {
                val track = Tracks[trackIndexs[0]]
                val blockRect = blockRects[0]
                val imageRect = if (track.isCenter) blockRect.second else blockRect.first
                val headProgress = currentState.headProgress
                // 拖尾
                drawTrailing(headProgress, 0, currentState.tailProgressList)
                // 按键
                noteTransform(headProgress, track) {
                    image(blockMap, imageRect, it)
                }
            }
            is State.Pressing -> {

            }
            is State.Releasing -> {

            }
            is State.Missing -> {

            }
        }
    }
}