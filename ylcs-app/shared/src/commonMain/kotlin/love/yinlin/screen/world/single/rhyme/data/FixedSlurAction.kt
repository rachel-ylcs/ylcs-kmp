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
import love.yinlin.compose.onLine
import love.yinlin.compose.translate
import love.yinlin.data.music.RhymeAction
import love.yinlin.screen.world.single.rhyme.RhymeAssets

@Stable
class FixedSlurAction(
    assets: RhymeAssets,
    private val start: Long,
    private val end: Long,
    override val action: RhymeAction.Slur
) : DynamicAction {
    @Stable
    private sealed interface State {
        @Stable
        data object Ready : State // 就绪
        @Stable
        class Moving : State { // 移动
            var headProgress by mutableFloatStateOf(0f)
            var tailProgress by mutableFloatStateOf(0f)
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
            val animation = LineFrameAnimation(frameCount, true).also { it.start() }
        }
        @Stable
        class Releasing(
            val pressTick: Long, // 抬起时间
            val releaseTick: Long, // 释放时间
            val result: ActionResult, // 按下结果
        ) : State { // 释放中
            val animation = LineFrameAnimation(30).also { it.start() }
        }
        @Stable
        class Missing(
            frameCount: Int,
            lastHeadProgress: Float,
            lastTailProgress: Float
        ) : State { // 错过中
            var headProgress by mutableFloatStateOf(lastHeadProgress)
            var tailProgress by mutableFloatStateOf(lastTailProgress)
            val animation = LineFrameAnimation(frameCount / 2).also { it.start() }
        }
        @Stable
        data object Done : State // 已完成
    }

    private val blockMap = assets.blockMap()
    private val longPress = assets.longPress()
    private val noteDismiss = assets.noteDismiss()

    private val trackIndex = DynamicAction.mapTrackIndex(action.scale.first())
    private val noteScale = DynamicAction.mapNoteScale(action.scale.first())
    private val blockRect = DynamicAction.calcBlockRect(noteScale, 3)

    private var state: State by mutableStateOf(State.Ready) // 状态

    override var bindId: Long? = null

    override val appearance: Long = start - (DynamicAction.BASE_DURATION * DynamicAction.HIT_RATIO.asUncheckedVirtual).toLong()
    private val actionDuration = end - start
    private val tailAppearance: Long = appearance + actionDuration

    override val isDismiss: Boolean by derivedStateOf { state == State.Done }

    override fun onAdmission() { state = State.Moving() }

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
                val headProgress = ((tick - appearance) / DynamicAction.BASE_DURATION_F).asActual
                val tailProgress = ((tick - tailAppearance) / DynamicAction.BASE_DURATION_F).asActual
                currentState.headProgress = headProgress
                currentState.tailProgress = tailProgress
                // 超出死线仍未处理的音符标记错过
                if (headProgress > DynamicAction.deadline) {
                    state = State.Missing(noteDismiss.frameCount, headProgress, tailProgress)
                    callback.updateResult(ActionResult.MISS)
                }
            }
            is State.Pressing -> {
                currentState.animation.update()
                currentState.tailProgress = ((tick - tailAppearance) / DynamicAction.BASE_DURATION_F).asActual
                // 尾部到达头部时自动结算
                if (currentState.tailProgress > currentState.lastHeadProgress) {
                    state = State.Releasing(currentState.pressTick, tick, currentState.result).also {
                        callback.calcResult(it)
                    }
                }
            }
            is State.Releasing -> {
                if (!currentState.animation.update()) state = State.Done
            }
            is State.Missing -> {
                if (!currentState.animation.update()) state = State.Done
                currentState.headProgress = ((tick - appearance) / DynamicAction.BASE_DURATION_F).asUncheckedActual.coerceAtMost(0.95f)
                // 加速尾部下落, 防止按键消失后突显拖尾
                val fastRatio = 1 + actionDuration / DynamicAction.BASE_DURATION_F
                currentState.tailProgress = ((tick - tailAppearance) / DynamicAction.BASE_DURATION_F * fastRatio).asUncheckedActual.coerceAtMost(0.95f)
            }
        }
    }

    override fun onTrackDown(track: Track, tick: Long, callback: ActionCallback): Boolean {
        // 校验轨道一致, 状态移动中
        val currentState = state
        if (track.index != trackIndex || currentState !is State.Moving) return false
        val headProgress = currentState.headProgress
        val tailProgress = currentState.tailProgress
        return ActionResult.inRange(DynamicAction.HIT_RATIO, headProgress)?.also { result ->
            if (result == ActionResult.MISS) { // 错过
                state = State.Missing(noteDismiss.frameCount, headProgress, tailProgress)
                callback.updateResult(result)
            }
            else state = State.Pressing(
                longPress.frameCount,
                pressTick = tick,
                result = result,
                lastHeadProgress = headProgress,
                lastTailProgress = tailProgress
            ) // 长按
        } != null
    }

    override fun onTrackUp(track: Track, tick: Long, callback: ActionCallback) {
        val currentState = state
        // 状态必须是按下
        if (track.index == trackIndex && currentState is State.Pressing) {
            state = State.Releasing(currentState.pressTick, tick, currentState.result).also {
                callback.calcResult(it)
            }
        }
    }

    override fun onTrackTransfer(oldTrack: Track, newTrack: Track, tick: Long, callback: ActionCallback): Boolean = false

    private fun Drawer.drawTrailing(track: Track, headProgress: Float, tailProgress: Float) {
        if (headProgress > tailProgress) {
            // 绘制拖尾
            val headLeft = Tracks.Vertices.onLine(track.bottomTailLeft, headProgress)
            val headRight = Tracks.Vertices.onLine(track.bottomTailRight, headProgress)
            val tailLeft = Tracks.Vertices.onLine(track.bottomTailLeft, tailProgress)
            val tailRight = Tracks.Vertices.onLine(track.bottomTailRight, tailProgress)
            path(
                brush = DynamicAction.SlurTailBrushes[noteScale],
                path = Path(arrayOf(headLeft, tailLeft, tailRight, headRight))
            )
            // 绘制侧边
            val sideWidth = 10f
            path(
                color = Colors.Ghost.copy(alpha = 0.8f),
                path = Path(arrayOf(headLeft, tailLeft, tailLeft.translate(x = sideWidth * tailProgress), headLeft.translate(x = sideWidth * headProgress)))
            )
            path(
                color = Colors.Ghost.copy(alpha = 0.8f),
                path = Path(arrayOf(headRight, tailRight, tailRight.translate(x = -sideWidth * tailProgress), headRight.translate(x = -sideWidth * headProgress)))
            )
            // 绘制终端
            val miniTailProgress = tailProgress + 0.01f
            if (headProgress > miniTailProgress) {
                val terminalLeft = Tracks.Vertices.onLine(track.bottomTailLeft, miniTailProgress)
                val terminalRight = Tracks.Vertices.onLine(track.bottomTailRight, miniTailProgress)
                path(
                    color = Colors.Ghost.copy(alpha = 0.8f),
                    path = Path(arrayOf(terminalLeft, tailLeft, tailRight, terminalRight))
                )
            }
        }
    }

    override fun Drawer.onDraw() {
        val currentState = state
        if (currentState == State.Ready || currentState == State.Done) return // 就绪或完成状态不渲染

        val track = Tracks[trackIndex]
        val imgRect = if (track.isCenter) blockRect.second else blockRect.first

        when (currentState) {
            is State.Moving -> {
                // 拖尾
                val headProgress = currentState.headProgress
                drawTrailing(track, headProgress, currentState.tailProgress)

                // 按键
                noteTransform(headProgress, track) {
                    image(blockMap, imgRect, it)
                }
            }
            is State.Pressing -> {
                // 拖尾
                val headProgress = currentState.lastHeadProgress
                drawTrailing(track, headProgress, currentState.tailProgress)

                noteTransform(headProgress, track) {
                    image(blockMap, imgRect, it)
                }

                // 动画
                drawPlainAnimation(track, DynamicAction.HIT_RATIO, longPress, currentState.animation, scaleRatio = 1.5f, colorFilter = DynamicAction.SlurColorFilters[noteScale])
            }
            is State.Releasing -> { } // 暂定为空
            is State.Missing -> {
                // 拖尾
                val headProgress = currentState.headProgress
                val tailProgress = currentState.tailProgress
                drawTrailing(track, headProgress, tailProgress)

                // 按键
                noteTransform(headProgress, track) {
                    image(blockMap, imgRect, it, alpha = (1 - currentState.animation.progress * 1.5f).coerceAtLeast(0f))
                    drawPerspectiveAnimation(track, it, noteDismiss, currentState.animation, colorFilter = DynamicAction.SlurColorFilters[noteScale])
                }
            }
        }
    }
}