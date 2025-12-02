package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Event
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.Transform
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
interface ActionCallback {
    fun updateScore(score: Int) // 更新得分
    fun updateCombo(result: ComboActionResult) // 更新连击
}

@Stable
sealed class DynamicAction {
    abstract val action: RhymeAction // 行为
    abstract val appearance: Long // 出现时刻

    abstract fun onAdmission() // 入场
    abstract fun isDismiss(): Boolean // 消失
    abstract fun onUpdate(tick: Long, callback: ActionCallback) // 更新
    abstract fun Drawer.onDraw(tracks: List<Track>, blockMap: ImageBitmap) // 渲染
}

@Stable
class NoteAction(start: Long, end: Long, override val action: RhymeAction.Note) : DynamicAction() {
    @Stable
    enum class State {
        Ready, // 就绪
        Moving, // 移动
        Clicking, // 点击中
        Missing, // 错过中
        Done; // 已完成
    }

    companion object {
        // 单音符时长与实际字符发音时长无关, 全部为固定值
        private const val DURATION = 2500L

        private const val PERFECT_RATIO = 0.25f
        private const val GOOD_RATIO = 0.5f
        private const val BAD_RATIO = 1f
        private const val MISS_RATIO = 3f
    }

    override val appearance: Long = start - (DURATION * Track.CLICK_CENTER_RATIO).toLong()

    private val trackIndex = when (val v = (action.scale - 1) % 7) {
        in 5 .. 7 -> v - 1
        else -> 4 - v
    }
    private val noteScale = (action.scale - 1) / 7
    private val blockSize = Size(256f, 85f)
    private val blockRect = Rect(Offset(0f, noteScale * blockSize.height), blockSize) to Rect(Offset(blockSize.width, noteScale * blockSize.height), blockSize)

    private var state: State by mutableStateOf(State.Ready) // 状态
    private var progress: Float by mutableFloatStateOf(0f) // 进度

    override fun onAdmission() {
        state = State.Moving
    }

    override fun isDismiss(): Boolean = state == State.Done

    override fun onUpdate(tick: Long, callback: ActionCallback) {
        when (state) {
            State.Ready, State.Done -> { } // 就绪或完成状态不更新
            State.Moving -> {
                progress = ((tick - appearance) / DURATION.toFloat()).coerceIn(0f, 1f)
                if (tick >= appearance + DURATION) { // 超出时长仍未处理标记错过
                    state = State.Missing
                }
            }
            State.Clicking -> {

            }
            State.Missing -> {
                state = State.Done
            }
        }
    }

    override fun Drawer.onDraw(tracks: List<Track>, blockMap: ImageBitmap) {
        val currentState = state
        if (currentState == State.Ready || currentState == State.Done) return // 就绪或完成状态不渲染

        val track = tracks[trackIndex]
        val (matrix, srcRect, _) = track.perspectiveMatrix
        transform({
            scale(progress, progress, track.vertices)
            transform(matrix)
            if (track.isRight) scale(-1f, 1f, srcRect.center)
        }) {
            image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect)
        }
    }
}

@Stable
class FixedSlurAction(start: Long, end: Long, override val action: RhymeAction.Slur) : DynamicAction() {
    companion object {
        private const val DURATION_BASE_RATIO = 5L
        private const val LENGTH_RATIO = 0.7f
        private const val HEADER_RATIO = 0.1f
    }

    override val appearance: Long = 0L

    override fun onAdmission() {

    }

    override fun isDismiss(): Boolean = false

    override fun onUpdate(tick: Long, callback: ActionCallback) {

    }

    override fun Drawer.onDraw(tracks: List<Track>, blockMap: ImageBitmap) {

    }
}

@Stable
class OffsetSlurAction(start: Long, end: Long, override val action: RhymeAction.Slur) : DynamicAction() {
    override val appearance: Long = 0L

    override fun onAdmission() {

    }

    override fun isDismiss(): Boolean = false

    override fun onUpdate(tick: Long, callback: ActionCallback) {

    }

    override fun Drawer.onDraw(tracks: List<Track>, blockMap: ImageBitmap) {

    }
}

@Stable
class NoteQueue(
    rhymeManager: RhymeManager,
    lyricsConfig: RhymeLyricsConfig,
    trackMap: TrackMap,
) : Spirit(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = trackMap.preTransform
    override val size: Size = trackMap.size

    private val lyrics = lyricsConfig.lyrics
    private val tracks = trackMap.tracks

    private val blockMap: ImageBitmap by manager.assets()

    // 预编译队列
    private val queue: List<DynamicAction> = buildList(lyrics.size) {
        for (line in lyrics) {
            val theme = line.theme
            for (i in theme.indices) {
                val action = theme[i]
                val start = (theme.getOrNull(i - 1)?.end ?: 0) + line.start
                val end = action.end + line.start
                val dynamicAction = when (action) {
                    is RhymeAction.Note -> NoteAction(start, end, action) // 单音
                    is RhymeAction.Slur -> {
                        val first = action.scale.firstOrNull()
                        if (action.scale.all { it == first }) FixedSlurAction(start, end, action) // 延音
                        else OffsetSlurAction(start, end, action) // 连音
                    }
                }
                add(dynamicAction)
            }
        }
    }

    // 入场指针
    private var actionIndex by mutableIntStateOf(-1)

    private inline fun foreachAction(block: (DynamicAction) -> Unit) {
        if (actionIndex >= 0) {
            for (index in 0 .. actionIndex) {
                val action = queue[index]
                // 只有未消失的字符才需要更新或渲染
                if (!action.isDismiss()) block(action)
            }
        }
    }

    // 音符行为回调
    private val callback = object : ActionCallback {
        override fun updateScore(score: Int) {

        }

        override fun updateCombo(result: ComboActionResult) {

        }
    }

    override fun onClientUpdate(tick: Long) {
        // 处理音符进入 (预编译队列入场顺序严格按照时间顺序)
        queue.getOrNull(actionIndex + 1)?.let { nextAction ->
            if (tick > nextAction.appearance) { // 到达出现时间
                // 入场
                nextAction.onAdmission()
                actionIndex++
            }
        }
        // 更新音符
        foreachAction { it.onUpdate(tick, callback) }
    }

    override fun onClientEvent(event: Event): Boolean {
        return false
    }

    override fun Drawer.onClientDraw() {
        foreachAction { it.apply { onDraw(tracks, blockMap) } }
    }
}