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
import love.yinlin.compose.game.animation.LineFrameAnimation
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Event
import love.yinlin.compose.game.traits.PointerDownEvent
import love.yinlin.compose.game.traits.PointerEvent
import love.yinlin.compose.game.traits.PointerUpEvent
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.Transform
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
interface ActionCallback {
    fun updateResult(result: ActionResult) // 更新连击
}

@Stable
sealed interface DynamicAction {
    companion object {
        const val PERSPECTIVE_K = 3 // 透视参数
        const val HIT_RATIO = 0.8f // 判定线
        const val BODY_RATIO = 0.05f // 自身比例
        val deadline = ActionResult.BAD.endRange(HIT_RATIO) // 死线
    }

    val action: RhymeAction // 行为
    val appearance: Long // 出现时刻

    fun onAdmission() // 入场
    fun isDismiss(): Boolean // 消失
    fun onUpdate(tick: Long, callback: ActionCallback) // 更新
    fun onPointerDown(track: Track, tick: Long, callback: ActionCallback): Boolean // 按下
    fun onPointerUp(track: Track, tick: Long, callback: ActionCallback): Boolean // 抬起
    fun Drawer.onDraw(tracks: List<Track>, blockMap: ImageBitmap) // 渲染
}

private val Float.asActual get() = 1 / (DynamicAction.PERSPECTIVE_K / this + 1 - DynamicAction.PERSPECTIVE_K)
private val Float.asVirtual get() = DynamicAction.PERSPECTIVE_K / (1 / this + DynamicAction.PERSPECTIVE_K - 1)

@Stable
class NoteAction(start: Long, end: Long, override val action: RhymeAction.Note) : DynamicAction {
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
        private const val DURATION = 3000L
    }

    override val appearance: Long = start - (DURATION * DynamicAction.HIT_RATIO.asVirtual).toLong()

    private val trackIndex = when (val v = (action.scale - 1) % 7) {
        in 5 .. 7 -> v - 1
        else -> 4 - v
    }
    private val noteScale = (action.scale - 1) / 7
    private val blockSize = Size(256f, 85f)
    private val blockRect = Rect(Offset(0f, noteScale * blockSize.height), blockSize) to Rect(Offset(blockSize.width, noteScale * blockSize.height), blockSize)

    private var state: State by mutableStateOf(State.Ready) // 状态
    private var progress: Float by mutableFloatStateOf(0f) // 进度
    private var animation = LineFrameAnimation(30) // 动画

    override fun onAdmission() {
        state = State.Moving
    }

    override fun isDismiss(): Boolean = state == State.Done

    override fun onUpdate(tick: Long, callback: ActionCallback) {
        when (state) {
            State.Ready, State.Done -> { } // 就绪或完成状态不更新
            State.Moving -> {
                // 更新进度
                progress = ((tick - appearance) / DURATION.toFloat()).asActual.coerceIn(0f, 1f)
                // 超出死线仍未处理的音符标记错过
                if (progress > DynamicAction.deadline) {
                    state = State.Missing
                    animation.start()
                    callback.updateResult(ActionResult.MISS)
                }
            }
            State.Clicking -> {
                // 动画时间到，切换完成状态
                if (!animation.update()) state = State.Done
            }
            State.Missing -> {
                // 动画时间到，切换完成状态
                if (!animation.update()) state = State.Done
            }
        }
    }

    override fun onPointerDown(track: Track, tick: Long, callback: ActionCallback): Boolean {
        // 校验轨道一致, 状态移动中
        if (track.index != trackIndex || state != State.Moving) return false
        val result = when {
            ActionResult.PERFECT.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.PERFECT
            ActionResult.GOOD.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.GOOD
            ActionResult.BAD.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.BAD
            ActionResult.MISS.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.MISS
            else -> null
        }
        return result?.also {
            state = if (it == ActionResult.MISS) State.Missing else State.Clicking
            animation.start()
            callback.updateResult(it)
        } != null
    }

    override fun onPointerUp(track: Track, tick: Long, callback: ActionCallback): Boolean = false

    override fun Drawer.onDraw(tracks: List<Track>, blockMap: ImageBitmap) {
        val currentState = state
        if (currentState == State.Ready || currentState == State.Done) return // 就绪或完成状态不渲染

        val track = tracks[trackIndex]
        val (matrix, srcRect, _) = track.perspectiveMatrix
        // 实际进度
        transform({
            // 先将轨道底部的画布以顶点为中心缩放到指定进度上
            scale(progress, track.vertices)
            // 然后透视
            transform(matrix)
            // 再根据轨道左右位置决定是否水平翻转
            if (track.isRight) scale(-1f, 1f, srcRect.center)
        }) {
            when (currentState) {
                State.Moving -> {
                    image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect)
                }
                State.Clicking -> {
                    // 在 progress 处停滞, 播放消失动画(先用淡出模拟)
                    image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect, alpha = 1 - animation.progress)
                }
                State.Missing -> {
                    // 在 progress 处停滞, 播放消失动画(先用淡出模拟)
                    image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect, alpha = 1 - animation.progress)
                }
            }
        }
    }
}

@Stable
class FixedSlurAction(start: Long, end: Long, override val action: RhymeAction.Slur) : DynamicAction {
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

    override fun onPointerDown(track: Track, tick: Long, callback: ActionCallback): Boolean = false

    override fun onPointerUp(track: Track, tick: Long, callback: ActionCallback): Boolean = false

    override fun Drawer.onDraw(tracks: List<Track>, blockMap: ImageBitmap) {

    }
}

@Stable
class OffsetSlurAction(start: Long, end: Long, override val action: RhymeAction.Slur) : DynamicAction {
    override val appearance: Long = 0L

    override fun onAdmission() {

    }

    override fun isDismiss(): Boolean = false

    override fun onUpdate(tick: Long, callback: ActionCallback) {

    }

    override fun onPointerDown(track: Track, tick: Long, callback: ActionCallback): Boolean = false

    override fun onPointerUp(track: Track, tick: Long, callback: ActionCallback): Boolean = false

    override fun Drawer.onDraw(tracks: List<Track>, blockMap: ImageBitmap) {

    }
}

@Stable
class NoteQueue(
    rhymeManager: RhymeManager,
    lyricsConfig: RhymeLyricsConfig,
    private val scoreBoard: ScoreBoard,
    private val comboBoard: ComboBoard,
    private val trackMap: TrackMap,
    private val screenEnvironment: ScreenEnvironment,
) : Spirit(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = trackMap.preTransform
    override val size: Size = trackMap.size

    private val audioDelay = rhymeManager.config.audioDelay

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

    // 入场指针索引
    private var actionIndex by mutableIntStateOf(-1)

    // 遍历场内音符
    private inline fun foreachAction(block: (DynamicAction) -> Boolean) {
        if (actionIndex >= 0) {
            for (index in 0 .. actionIndex) {
                val action = queue[index]
                // 只有未消失的字符才需要更新或渲染
                if (!action.isDismiss()) {
                    if (block(action)) break
                }
            }
        }
    }

    // 音符行为回调
    private val callback = object : ActionCallback {
        override fun updateResult(result: ActionResult) {
            // 更新连击和分数
            val score = comboBoard.updateAction(result)
            scoreBoard.addScore(score)
            // 更新环境
            if (result == ActionResult.MISS) screenEnvironment.missEnvironment.animation.start()
        }
    }

    override fun onClientUpdate(tick: Long) {
        val compensateTick = tick - audioDelay // 延时补偿
        // 处理音符进入 (预编译队列入场顺序严格按照时间顺序)
        queue.getOrNull(actionIndex + 1)?.let { nextAction ->
            if (compensateTick > nextAction.appearance) { // 到达出现时间
                // 入场
                nextAction.onAdmission()
                actionIndex++
            }
        }
        // 更新音符
        foreachAction {
            it.onUpdate(compensateTick, callback)
            false
        }
    }

    override fun onClientEvent(tick: Long, event: Event): Boolean {
        val compensateTick = tick - audioDelay // 延时补偿
        return when (event) {
            is PointerEvent -> {
                // 获取实际轨道索引
                trackMap.calcTrackIndex(event.position)?.also { track ->
                    val trackIndex = track.index
                    val activeTracks = trackMap.activeTracks
                    when (event) {
                        is PointerDownEvent -> {
                            // 防止多指按下统一轨道
                            if (activeTracks[trackIndex] == null) {
                                activeTracks[trackIndex] = event.id
                                foreachAction { it.onPointerDown(track, compensateTick, callback) }
                            }
                        }
                        is PointerUpEvent -> {
                            if (activeTracks.indexOfFirst { it == event.id } == trackIndex) {
                                // 抬起时回调先于置空
                                foreachAction { it.onPointerUp(track, compensateTick, callback) }
                                activeTracks[trackIndex] = null
                            }
                        }
                    }
                } != null
            }
        }
    }

    override fun Drawer.onClientDraw() {
        foreachAction {
            it.apply { onDraw(tracks, blockMap) }
            false
        }
    }
}