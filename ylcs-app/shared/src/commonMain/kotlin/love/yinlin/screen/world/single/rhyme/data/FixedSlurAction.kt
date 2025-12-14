package love.yinlin.screen.world.single.rhyme.data

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import love.yinlin.compose.Colors
import love.yinlin.compose.Path
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.animation.LineFrameAnimation
import love.yinlin.compose.game.animation.SpeedAdapter
import love.yinlin.compose.onLine
import love.yinlin.compose.translate
import love.yinlin.data.music.RhymeAction
import love.yinlin.screen.world.single.rhyme.RhymeAssets
import love.yinlin.screen.world.single.rhyme.RhymePlayConfig

@Stable
class FixedSlurAction(
    assets: RhymeAssets,
    playConfig: RhymePlayConfig,
    start: Long,
    end: Long,
    override val action: RhymeAction.Slur
) : DynamicAction(assets, playConfig, start, end) {
    @Stable
    private sealed interface State {
        @Stable
        data object Ready : State // 就绪
        @Stable
        class Moving(
            private val duration: Long,
            private val actionDuration: Long,
        ) : State { // 移动
            // 头部进度
            var headProgress by mutableFloatStateOf(0f)
            // 尾部计算公式 tail = A[(V[head] * D - AD) / D]
            val tailProgress: Float get() = ((headProgress.asUncheckedVirtual * duration - actionDuration) / duration).asUncheckedActual
        }
        @Stable
        class Pressing(
            frameCount: Int,
            val pressTick: Long, // 按下时间
            val result: ActionResult, // 按下结果
            val lastHeadProgress: Float,
            lastTailProgress: Float,
        ) : State { // 长按中
            var tailProgress by mutableFloatStateOf(lastTailProgress)
            val blockAnimation = LineFrameAnimation(frameCount / 2).also { it.start() }
            val animation = LineFrameAnimation(frameCount, true).also { it.start() }
        }
        @Stable
        class Releasing(
            frameCount: Int,
            val pressTick: Long, // 抬起时间
            val releaseTick: Long, // 释放时间
            val result: ActionResult, // 按下结果
            val lastHeadProgress: Float,
            val lastTailProgress: Float,
        ) : State { // 释放中
            val animation = LineFrameAnimation(frameCount).also { it.start() }
        }
        @Stable
        class Missing(
            frameCount: Int,
            lastHeadProgress: Float,
            lastTailProgress: Float,
        ) : State { // 错过中
            var headProgress by mutableFloatStateOf(lastHeadProgress)
            var tailProgress by mutableFloatStateOf(lastTailProgress)
            val animation = LineFrameAnimation(frameCount / 2, adapter = SpeedAdapter(0.5f)).also { it.start() }
        }
        @Stable
        data object Done : State // 已完成
    }

    private val trackIndex = mapTrackIndex(action.scale.first())
    private val noteScale = mapNoteScale(action.scale.first())
    private val blockRect = calcBlockRect(noteScale + 3)

    private var state: State by mutableStateOf(State.Ready) // 状态

    override var bindId: Long? = null

    override val appearance: Long = start - (baseDuration * HIT_RATIO.asUncheckedVirtual).toLong()
    private val actionDuration = end - start // 长按总时长
    private val tailAppearance: Long = appearance + actionDuration

    override val isDismiss: Boolean by derivedStateOf { state == State.Done }

    override fun onAdmission() { state = State.Moving(baseDuration, actionDuration) }

    private fun ActionCallback.calcResult(state: State.Releasing) {
        // 计算长按持续时间
        val actualPressTick = state.pressTick.coerceAtLeast(start)
        val actualReleaseTick = state.releaseTick.coerceAtMost(end)
        val pressDuration = (actualReleaseTick - actualPressTick).coerceAtLeast(0L)
        // 系数计分规则
        val ratio = pressDuration / actionDuration.toFloat()
        val coefficient = (1 + 0.87f * ratio * ratio * (ratio + 1)).coerceIn(1f, 2f)
        updateResult(state.result, coefficient)
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
                    state = State.Missing(noteDismiss.frameCount, headProgress, currentState.tailProgress)
                    callback.updateResult(ActionResult.MISS)
                }
            }
            is State.Pressing -> {
                currentState.blockAnimation.update()
                currentState.animation.update()
                currentState.tailProgress = ((tick - tailAppearance) / baseDurationF).asActual
                val lastHeadProgress = currentState.lastHeadProgress
                val tailProgress = currentState.tailProgress
                // 尾部到达头部时自动结算
                if (tailProgress > lastHeadProgress) {
                    state = State.Releasing(
                        longRelease.frameCount,
                        currentState.pressTick,
                        tick,
                        currentState.result,
                        lastHeadProgress,
                        tailProgress
                    ).also {
                        callback.calcResult(it)
                    }
                }
            }
            is State.Releasing -> {
                if (!currentState.animation.update()) state = State.Done
            }
            is State.Missing -> {
                if (!currentState.animation.update()) state = State.Done
                currentState.headProgress = ((tick - appearance) / baseDurationF).asUncheckedActual.coerceAtMost(0.95f)
                currentState.tailProgress = ((tick - tailAppearance) / baseDurationF).asUncheckedActual.coerceAtMost(0.95f)
            }
        }
    }

    override fun onTrackDown(track: Track, tick: Long, callback: ActionCallback): Boolean {
        // 校验轨道一致, 状态移动中
        val currentState = state
        if (track.index != trackIndex || currentState !is State.Moving) return false
        val headProgress = currentState.headProgress
        val tailProgress = currentState.tailProgress
        return ActionResult.inRange(HIT_RATIO, headProgress)?.also { result ->
            state = if (result == ActionResult.MISS) { // 错过
                callback.updateResult(result)
                State.Missing(noteDismiss.frameCount, headProgress, tailProgress)
            }
            else { // 长按
                State.Pressing(
                    longPress.frameCount,
                    pressTick = tick,
                    result = result,
                    lastHeadProgress = headProgress,
                    lastTailProgress = tailProgress
                )
            }
        } != null
    }

    override fun onTrackUp(track: Track, tick: Long, callback: ActionCallback) {
        val currentState = state
        // 状态必须是按下
        if (track.index == trackIndex && currentState is State.Pressing) {
            state = State.Releasing(
                longRelease.frameCount,
                currentState.pressTick,
                tick,
                currentState.result,
                currentState.lastHeadProgress,
                currentState.tailProgress
            ).also {
                callback.calcResult(it)
            }
        }
    }

    override fun onTrackTransfer(oldTrack: Track, newTrack: Track, tick: Long, callback: ActionCallback): Boolean = false

    private fun Drawer.drawTrailing(track: Track, headProgress: Float, tailProgress: Float, alpha: Float = 1f) {
        if (headProgress <= tailProgress || alpha <= 0f) return
        // 绘制拖尾
        val headLeft = Tracks.Vertices.onLine(track.bottomTailLeft, headProgress)
        val headRight = Tracks.Vertices.onLine(track.bottomTailRight, headProgress)
        val tailLeft = Tracks.Vertices.onLine(track.bottomTailLeft, tailProgress)
        val tailRight = Tracks.Vertices.onLine(track.bottomTailRight, tailProgress)
        path(
            brush = SlurTailBrushes[noteScale],
            path = Path(arrayOf(headLeft, tailLeft, tailRight, headRight)),
            alpha = alpha
        )
        // 绘制侧边
        val sideWidth = 10f
        path(
            color = Colors.Ghost.copy(alpha = 0.8f),
            path = Path(arrayOf(headLeft, tailLeft, tailLeft.translate(x = sideWidth * tailProgress), headLeft.translate(x = sideWidth * headProgress))),
            alpha = alpha
        )
        path(
            color = Colors.Ghost.copy(alpha = 0.8f),
            path = Path(arrayOf(headRight, tailRight, tailRight.translate(x = -sideWidth * tailProgress), headRight.translate(x = -sideWidth * headProgress))),
            alpha = alpha
        )
        // 绘制终端
        val miniRatio = 0.01f
        val miniHeadProgress = headProgress - miniRatio
        val miniTailProgress = tailProgress + miniRatio
        if (tailProgress < miniHeadProgress) {
            val terminalLeft = Tracks.Vertices.onLine(track.bottomTailLeft, miniHeadProgress)
            val terminalRight = Tracks.Vertices.onLine(track.bottomTailRight, miniHeadProgress)
            path(
                color = Colors.Ghost.copy(alpha = 0.8f),
                path = Path(arrayOf(terminalLeft, headLeft, headRight, terminalRight)),
                alpha = alpha
            )
        }
        if (headProgress > miniTailProgress) {
            val terminalLeft = Tracks.Vertices.onLine(track.bottomTailLeft, miniTailProgress)
            val terminalRight = Tracks.Vertices.onLine(track.bottomTailRight, miniTailProgress)
            path(
                color = Colors.Ghost.copy(alpha = 0.8f),
                path = Path(arrayOf(terminalLeft, tailLeft, tailRight, terminalRight)),
                alpha = alpha
            )
        }
    }

    override fun Drawer.onDraw() {
        val currentState = state
        if (currentState == State.Ready || currentState == State.Done) return // 就绪或完成状态不渲染

        val track = Tracks[trackIndex]
        val imgRect = if (track.isCenter) blockRect.second else blockRect.first

        when (currentState) {
            is State.Moving -> {
                val headProgress = currentState.headProgress
                // 拖尾
                drawTrailing(track, headProgress, currentState.tailProgress)
                // 按键
                noteTransform(headProgress, track) {
                    image(blockMap, imgRect, it)
                }
            }
            is State.Pressing -> {
                val headProgress = currentState.lastHeadProgress
                // 拖尾
                drawTrailing(track, headProgress, currentState.tailProgress)
                // 按键
                val blockAlpha = (1 - currentState.blockAnimation.progress * 2f).coerceAtLeast(0f)
                noteTransform(headProgress, track) {
                    if (blockAlpha > 0f) image(blockMap, imgRect, it, alpha = blockAlpha)
                }
                // 动画
                drawPlainAnimation(track, headProgress, longPress, currentState.animation, scaleRatio = 1.25f, colorFilter = ResultColorFilters[currentState.result.ordinal])
            }
            is State.Releasing -> {
                val lastHeadProgress = currentState.lastHeadProgress
                val releasingAlpha = (1 - currentState.animation.progress * 1.5f).coerceAtLeast(0f)
                // 拖尾
                drawTrailing(track, lastHeadProgress, currentState.lastTailProgress, alpha = releasingAlpha)
                // 动画
                drawPlainAnimation(track, lastHeadProgress, longRelease, currentState.animation, colorFilter = ResultColorFilters[currentState.result.ordinal])
            }
            is State.Missing -> {
                val headProgress = currentState.headProgress
                val missAlpha = (1 - currentState.animation.progress * 2f).coerceAtLeast(0f)
                // 拖尾
                drawTrailing(track, headProgress, currentState.tailProgress, alpha = missAlpha)
                // 按键
                noteTransform(headProgress, track) {
                    if (missAlpha > 0f) image(blockMap, imgRect, it, alpha = missAlpha)
                    drawPerspectiveAnimation(track, it, noteDismiss, currentState.animation, colorFilter = SlurColorFilters[noteScale])
                }
            }
        }
    }
}