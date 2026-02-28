package love.yinlin.common.rhyme.data

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.animation.LineFrameAnimation
import love.yinlin.compose.game.animation.SpeedAdapter
import love.yinlin.data.music.RhymeAction
import love.yinlin.common.rhyme.RhymeAssets
import love.yinlin.common.rhyme.RhymePlayConfig

@Stable
class NoteAction(
    assets: RhymeAssets,
    playConfig: RhymePlayConfig,
    start: Long,
    end: Long,
    override val action: RhymeAction.Note
) : DynamicAction(assets, playConfig, start, end) {
    @Stable
    private sealed interface State {
        @Stable
        data object Ready : State // 就绪
        @Stable
        class Moving : State { // 移动
            var progress by mutableFloatStateOf(0f)
        }
        @Stable
        class Clicking(frameCount: Int, val result: ActionResult, val lastProgress: Float) : State { // 点击中
            val animation = LineFrameAnimation(frameCount).also { it.start() }
        }
        @Stable
        class Missing(frameCount: Int, lastProgress: Float) : State { // 错过中
            var progress by mutableFloatStateOf(lastProgress)
            val animation = LineFrameAnimation(frameCount / 2, adapter = SpeedAdapter(0.5f)).also { it.start() }
        }
        @Stable
        data object Done : State // 已完成
    }

    private val trackIndex = mapTrackIndex(action.scale)
    private val noteLevel = mapNoteLevel(action.scale)
    private val blockRect = calcBlockRect(noteLevel)

    private var state: State by mutableStateOf(State.Ready) // 状态

    override var bindId: Long? = null

    // A[(start - appearance) / duration] = hit
    // -> (start - appearance) / duration = V(hit)
    // -> start - appearance = duration * V(hit)
    // -> appearance = start - duration * V(hit)
    override val appearance: Long = start - (baseDuration * HIT_RATIO.asUncheckedVirtual).toLong()

    override val isDismiss: Boolean by derivedStateOf { state == State.Done }

    override fun onAdmission() { state = State.Moving() }

    override fun onUpdate(tick: Long, callback: ActionCallback) {
        when (val currentState = state) {
            State.Ready, State.Done -> { } // 就绪或完成状态不更新
            is State.Moving -> {
                // 更新进度
                val progress = ((tick - appearance) / baseDurationF).asActual
                currentState.progress = progress
                // 超出死线仍未处理的音符标记错过
                if (progress > deadline) {
                    state = State.Missing(noteDismiss.frameCount, progress)
                    callback.updateResult(ActionResult.MISS)
                }
            }
            is State.Clicking -> {
                // 动画时间到, 切换完成态
                if (!currentState.animation.update()) state = State.Done
            }
            is State.Missing -> {
                // 动画时间到, 切换完成态
                if (!currentState.animation.update()) state = State.Done
                currentState.progress = ((tick - appearance) / baseDurationF).asUncheckedActual.coerceAtMost(0.95f)
            }
        }
    }

    override fun onTrackDown(track: Track, tick: Long, callback: ActionCallback): Boolean {
        // 校验轨道一致, 状态移动中
        val currentState = state
        if (track.index != trackIndex || currentState !is State.Moving) return false
        val progress = currentState.progress
        return ActionResult.inRange(HIT_RATIO, progress)?.also { result ->
            // 切换点击态或错过态
            state = if (result == ActionResult.MISS) {
                State.Missing(noteDismiss.frameCount, progress)
            } else {
                callback.playSound(soundNoteClick)
                State.Clicking(noteClick.frameCount, result, progress)
            }
            callback.updateResult(result)
        } != null
    }

    override fun onTrackUp(track: Track, tick: Long, callback: ActionCallback) { }

    override fun onTrackTransfer(oldTrack: Track, newTrack: Track, tick: Long, callback: ActionCallback): Boolean = false

    override fun Drawer.onDraw() {
        val currentState = state
        if (currentState == State.Ready || currentState == State.Done) return // 就绪或完成状态不渲染

        val track = Tracks[trackIndex]
        val imgRect = if (track.isCenter) blockRect.second else blockRect.first

        when (currentState) {
            is State.Moving -> {
                // 按键
                noteTransform(currentState.progress, track) {
                    image(blockMap, imgRect, it)
                }
            }
            is State.Clicking -> {
                val lastProgress = currentState.lastProgress
                val blockAlpha = (1 - currentState.animation.progress * 1.5f).coerceAtLeast(0f)
                // 按键
                noteTransform(lastProgress, track) {
                    if (blockAlpha > 0f) image(blockMap, imgRect, it, alpha = blockAlpha)
                }
                // 动画
                drawPlainAnimation(track, lastProgress, noteClick, currentState.animation, scaleRatio = 1.25f, colorFilter = ResultColorFilters[currentState.result.ordinal])
            }
            is State.Missing -> {
                val missAlpha = (1 - currentState.animation.progress * 2f).coerceAtLeast(0f)
                // 按键
                noteTransform(currentState.progress, track) {
                    if (missAlpha > 0f) image(blockMap, imgRect, it, alpha = missAlpha)
                    drawPerspectiveAnimation(track, it, noteDismiss, currentState.animation, colorFilter = NoteColorFilters[noteLevel])
                }
            }
        }
    }
}