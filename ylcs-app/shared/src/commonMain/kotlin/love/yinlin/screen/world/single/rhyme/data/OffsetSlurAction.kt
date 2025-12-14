package love.yinlin.screen.world.single.rhyme.data

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.animation.LineFrameAnimation
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
    private sealed interface State {
        @Stable
        data object Ready : State // 就绪
        @Stable
        data object Moving : State // 移动
        @Stable
        data object Missing : State // 错过中
        @Stable
        data object Done : State // 已完成
    }

    // 长按总时长
    private val pressDuration = (end - start)
    // 每个音符长按时长
    private val perDuration = pressDuration / action.scale.size

    private val blockRect = calcBlockRect(0, 6)

    private var state: State by mutableStateOf(State.Ready) // 状态

    private var pressingAnimation = LineFrameAnimation(30, true) // 长按动画

    override var bindId: Long? = null

    override val appearance: Long = start - (baseDuration * HIT_RATIO.asVirtual).toLong()

    override val isDismiss: Boolean by derivedStateOf { state == State.Done }

    override fun onAdmission() { state = State.Moving }

    private fun ActionCallback.calcResult() {

    }

    override fun onUpdate(tick: Long, callback: ActionCallback) {

    }

    override fun onTrackDown(track: Track, tick: Long, callback: ActionCallback): Boolean {
        return false
    }

    override fun onTrackUp(track: Track, tick: Long, callback: ActionCallback) {

    }

    override fun onTrackTransfer(oldTrack: Track, newTrack: Track, tick: Long, callback: ActionCallback): Boolean {
        return false
    }

    override fun Drawer.onDraw() {

    }
}