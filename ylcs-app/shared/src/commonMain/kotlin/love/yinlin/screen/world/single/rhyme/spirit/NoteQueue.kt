package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.util.fastMapIndexed
import love.yinlin.compose.Colors
import love.yinlin.compose.Path
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.animation.LineFrameAnimation
import love.yinlin.compose.game.traits.*
import love.yinlin.compose.onLine
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
interface ActionCallback {
    fun updateResult(result: ActionResult, scoreRatio: Float = 1f) // 处理音符结果
}

@Stable
sealed interface DynamicAction {
    companion object {
        const val BASE_DURATION = 3000L // 音符持续时间
        const val PERSPECTIVE_K = 3 // 透视参数
        const val HIT_RATIO = 0.8f // 判定线
        const val BODY_RATIO = 0.05f // 自身比例

        val deadline = ActionResult.BAD.endRange(HIT_RATIO) // 死线

        val BlockSize = Size(256f, 85f)
        fun calcBlockRect(scale: Int, base: Int): Pair<Rect, Rect> {
            val left = Rect(Offset(0f, (scale + base) * BlockSize.height), BlockSize)
            val center = Rect(Offset(BlockSize.width, (scale + base) * BlockSize.height), BlockSize)
            return left to center
        }

        fun mapTrackIndex(scale: Byte): Int = Track.Scales.indexOf((scale - 1) % 7 + 1)

        fun mapNoteScale(scale: Byte): Int = (scale - 1) / 7
    }

    var bindId: Long? // 绑定指针ID
    val action: RhymeAction // 行为
    val appearance: Long // 出现时刻
    val isDismiss: Boolean // 消失

    fun onAdmission() // 入场
    fun onUpdate(tick: Long, callback: ActionCallback) // 更新
    fun onTrackDown(track: Track, tick: Long, callback: ActionCallback): Boolean // 按下
    fun onTrackUp(track: Track, tick: Long, callback: ActionCallback) // 抬起
    fun onTrackTransfer(oldTrack: Track, newTrack: Track, tick: Long, callback: ActionCallback): Boolean // 变轨
    fun onTrackMove(track: Track, offset: Float, tick: Long, callback: ActionCallback) // 移动
    fun Drawer.onDraw(tracks: List<Track>, blockMap: ImageBitmap) // 渲染
}

private val Float.asActual get() = 1 / (DynamicAction.PERSPECTIVE_K / this + 1 - DynamicAction.PERSPECTIVE_K)
private val Float.asVirtual get() = DynamicAction.PERSPECTIVE_K / (1 / this + DynamicAction.PERSPECTIVE_K - 1)

@Stable
class NoteAction(
    start: Long,
    override val action: RhymeAction.Note
) : DynamicAction {
    @Stable
    private enum class State {
        Ready, // 就绪
        Moving, // 移动
        Clicking, // 点击中
        Missing, // 错过中
        Done; // 已完成
    }

    private val trackIndex = DynamicAction.mapTrackIndex(action.scale)
    private val noteScale = DynamicAction.mapNoteScale(action.scale)
    private val blockRect = DynamicAction.calcBlockRect(noteScale, 0)
    private var state: State by mutableStateOf(State.Ready) // 状态
    private var progress: Float by mutableFloatStateOf(0f) // 进度
    private var clickingAnimation = LineFrameAnimation(30) // 点击动画
    private var missingAnimation = LineFrameAnimation(30) // 消失动画

    override var bindId: Long? = null

    override val appearance: Long = start - (DynamicAction.BASE_DURATION * DynamicAction.HIT_RATIO.asVirtual).toLong()

    override val isDismiss: Boolean by derivedStateOf { state == State.Done }

    override fun onAdmission() { state = State.Moving }

    override fun onUpdate(tick: Long, callback: ActionCallback) {
        when (state) {
            State.Ready, State.Done -> { } // 就绪或完成状态不更新
            State.Moving -> {
                // 更新进度
                progress = ((tick - appearance) / DynamicAction.BASE_DURATION.toFloat()).asActual.coerceIn(0f, 1f)
                // 超出死线仍未处理的音符标记错过
                if (progress > DynamicAction.deadline) {
                    state = State.Missing
                    missingAnimation.start()
                    callback.updateResult(ActionResult.MISS)
                }
            }
            State.Clicking -> {
                // 动画时间到，切换完成状态
                if (!clickingAnimation.update()) state = State.Done
            }
            State.Missing -> {
                progress = ((tick - appearance) / DynamicAction.BASE_DURATION.toFloat()).asActual.coerceIn(0f, 1f)
                if (!missingAnimation.update()) state = State.Done
            }
        }
    }

    override fun onTrackDown(track: Track, tick: Long, callback: ActionCallback): Boolean {
        // 校验轨道一致, 状态移动中
        if (track.index != trackIndex || state != State.Moving) return false
        return when {
            ActionResult.PERFECT.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.PERFECT
            ActionResult.GOOD.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.GOOD
            ActionResult.BAD.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.BAD
            ActionResult.MISS.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.MISS
            else -> null
        }?.also { result ->
            if (result == ActionResult.MISS) { // 错过
                state = State.Missing
                missingAnimation.start()
            }
            else { // 完成点击
                state = State.Clicking
                clickingAnimation.start()
            }
            callback.updateResult(result)
        } != null
    }

    override fun onTrackUp(track: Track, tick: Long, callback: ActionCallback) { }

    override fun onTrackTransfer(oldTrack: Track, newTrack: Track, tick: Long, callback: ActionCallback): Boolean = false

    override fun onTrackMove(track: Track, offset: Float, tick: Long, callback: ActionCallback) { }

    override fun Drawer.onDraw(tracks: List<Track>, blockMap: ImageBitmap) {
        val currentState = state
        if (currentState == State.Ready || currentState == State.Done) return // 就绪或完成状态不渲染

        val track = tracks[trackIndex]
        val (matrix, srcRect, _) = track.perspectiveMatrix
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
                    image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect, alpha = 1 - clickingAnimation.progress)
                }
                State.Missing -> {
                    // 在 progress 处停滞, 播放消失动画(先用淡出模拟)
                    image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect, alpha = 1 - missingAnimation.progress)
                }
            }
        }
    }
}

@Stable
class FixedSlurAction(
    private val start: Long,
    private val end: Long,
    override val action: RhymeAction.Slur
) : DynamicAction {
    @Stable
    private sealed interface State {
        @Stable
        data object Ready : State // 就绪
        @Stable
        data object Moving : State // 移动
        @Stable
        data class Pressing(
            val pressTick: Long, // 按下时间
            val result: ActionResult, // 按下结果
        ) : State // 长按中
        @Stable
        data class Releasing(
            val pressTick: Long, // 抬起时间
            val releaseTick: Long, // 释放时间
            val result: ActionResult, // 按下结果
        ) : State // 释放中
        @Stable
        data object Missing : State // 错过中
        @Stable
        data object Done : State // 已完成
    }

    companion object {
        val brush = arrayOf(
            listOf(Colors.Red5.copy(alpha = 0.8f), Colors.Red5.copy(alpha = 0.2f), Colors.Transparent),
            listOf(Colors.Orange6.copy(alpha = 0.8f), Colors.Orange6.copy(alpha = 0.4f), Colors.Transparent),
            listOf(Colors.Cyan6.copy(alpha = 0.8f), Colors.Cyan6.copy(alpha = 0.4f), Colors.Transparent)
        )
    }

    private val trackIndex = DynamicAction.mapTrackIndex(action.scale.first())
    private val noteScale = DynamicAction.mapNoteScale(action.scale.first())
    private val blockRect = DynamicAction.calcBlockRect(noteScale, 3)

    private var state: State by mutableStateOf(State.Ready) // 状态
    private var progress: Float by mutableFloatStateOf(0f) // 进度

    private var tailRatio by mutableFloatStateOf((end - start) / DynamicAction.BASE_DURATION.toFloat())

    private var pressingAnimation = LineFrameAnimation(30, true) // 长按动画
    private var releasingAnimation = LineFrameAnimation(30) // 释放动画
    private var missingAnimation = LineFrameAnimation(30) // 消失动画

    override var bindId: Long? = null

    override val appearance: Long = start - (DynamicAction.BASE_DURATION * DynamicAction.HIT_RATIO.asVirtual).toLong()

    override val isDismiss: Boolean by derivedStateOf { state == State.Done }

    override fun onAdmission() { state = State.Moving }

    private fun ActionCallback.calcResult(state: State.Releasing) {
        // 计算长按持续时间
        val actualPressTick = state.pressTick.coerceAtLeast(start)
        val actualReleaseTick = state.releaseTick.coerceAtMost(end)
        val pressDuration = (actualReleaseTick - actualPressTick).coerceAtLeast(0L)
        // 系数计分规则
        val ratio = pressDuration / (end - start).toFloat()
        val coefficient = (1 + 0.87f * ratio * ratio * (ratio + 1)).coerceIn(1f, 2f)
        updateResult(state.result, coefficient)
    }

    override fun onUpdate(tick: Long, callback: ActionCallback) {
        when (val currentState = state) {
            State.Ready, State.Done -> { } // 就绪或完成状态不更新
            State.Moving -> {
                // 更新进度
                progress = ((tick - appearance) / DynamicAction.BASE_DURATION.toFloat()).asActual.coerceIn(0f, 1f)
                // 超出死线仍未处理的音符标记错过
                if (progress > DynamicAction.deadline) {
                    state = State.Missing
                    missingAnimation.start()
                    callback.updateResult(ActionResult.MISS)
                }
            }
            is State.Pressing -> {
                // 保持进度不变, 减小拖尾长度
                tailRatio = (end - tick) / DynamicAction.BASE_DURATION.toFloat()
                // 超过长按长度自动结算
                if (tailRatio <= 0f) {
                    val releasingState = State.Releasing(currentState.pressTick, tick, currentState.result)
                    state = releasingState
                    releasingAnimation.start()
                    callback.calcResult(releasingState)
                }
            }
            is State.Releasing -> {
                // 动画时间到，切换完成状态
                if (!releasingAnimation.update()) state = State.Done
            }
            State.Missing -> {
                progress = ((tick - appearance) / DynamicAction.BASE_DURATION.toFloat()).asActual.coerceIn(0f, 1f)
                if (!missingAnimation.update()) state = State.Done
            }
        }
    }

    override fun onTrackDown(track: Track, tick: Long, callback: ActionCallback): Boolean {
        // 校验轨道一致, 状态移动中
        if (track.index != trackIndex || state != State.Moving) return false

        return when {
            ActionResult.PERFECT.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.PERFECT
            ActionResult.GOOD.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.GOOD
            ActionResult.BAD.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.BAD
            ActionResult.MISS.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.MISS
            else -> null
        }?.also { result ->
            if (result == ActionResult.MISS) { // 错过
                state = State.Missing
                missingAnimation.start()
                callback.updateResult(result)
            }
            else { // 长按
                state = State.Pressing(pressTick = tick, result = result)
                pressingAnimation.start()
            }
        } != null
    }

    override fun onTrackUp(track: Track, tick: Long, callback: ActionCallback) {
        val currentState = state
        // 状态必须是按下
        require(track.index == trackIndex && currentState is State.Pressing)

        val releasingState = State.Releasing(currentState.pressTick, tick, currentState.result)
        state = releasingState
        releasingAnimation.start()
        callback.calcResult(releasingState)
    }

    override fun onTrackTransfer(oldTrack: Track, newTrack: Track, tick: Long, callback: ActionCallback): Boolean = false

    override fun onTrackMove(track: Track, offset: Float, tick: Long, callback: ActionCallback) { }

    override fun Drawer.onDraw(tracks: List<Track>, blockMap: ImageBitmap) {
        val currentState = state
        if (currentState == State.Ready || currentState == State.Done) return // 就绪或完成状态不渲染

        val track = tracks[trackIndex]
        val (matrix, srcRect, _) = track.perspectiveMatrix

        // 拖尾
        when (currentState) {
            State.Moving, is State.Pressing -> {
                val tailEndRatio = (progress.asVirtual - tailRatio).coerceIn(0f, 1f).asActual
                val startLeft = track.vertices.onLine(track.left, progress)
                val startRight = track.vertices.onLine(track.right, progress)
                val endLeft = track.vertices.onLine(track.left, tailEndRatio)
                val endRight = track.vertices.onLine(track.right, tailEndRatio)
                // 尾部起始连线的六等分点
                path(
                    brush = Brush.verticalGradient(brush[noteScale], startY = startLeft.y, endY = endLeft.y),
                    path = Path(arrayOf(
                        startLeft.onLine(startRight, 0.166667f),
                        endLeft.onLine(endRight, 0.166667f),
                        endLeft.onLine(endRight, 0.833333f),
                        startLeft.onLine(startRight, 0.833333f)
                    ))
                )
            }
            else -> { }
        }

        transform({
            scale(progress, track.vertices)
            transform(matrix)
            if (track.isRight) scale(-1f, 1f, srcRect.center)
        }) {
            when (currentState) {
                State.Moving -> {
                    image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect)
                }
                is State.Pressing -> {
                    image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect)
                }
                is State.Releasing -> {
                    image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect, alpha = 1 - releasingAnimation.progress)
                }
                State.Missing -> {
                    image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect, alpha = 1 - missingAnimation.progress)
                }
            }
        }
    }
}

@Stable
class OffsetSlurAction(
    private val start: Long,
    private val end: Long,
    override val action: RhymeAction.Slur
) : DynamicAction {
    @Stable
    private enum class State {
        Ready, // 就绪
        Moving, // 移动
        Pressing, // 长按中
        Transfering, // 变轨中
        Releasing, // 释放中
        Missing, // 释放中
        Done; // 已完成
    }

    // 变轨信息
    @Stable
    private class TransferInfo(
        val isBeginning: Boolean, // 初始轨道
        val tick: Long, // 按下时刻
        val noteScale: Int, // 音阶
        val trackIndex: Int, // 轨道索引
        val blockRect: Pair<Rect, Rect> // 主体图区域
    ) {
        companion object {
            const val TRANSFER_DURATION = 100L // 变轨时长
        }

        val startTick = tick - TRANSFER_DURATION
        val endTick = tick + TRANSFER_DURATION

        var pressTick: Long? = null
        var releaseTick: Long? = null
    }

    // 变轨信息
    private val infos = run {
        val perDuration = (end - start) / action.scale.size
        action.scale.fastMapIndexed { index, scale ->
            val noteScale = DynamicAction.mapNoteScale(scale)
            TransferInfo(
                isBeginning = index == 0,
                tick = start + perDuration * index,
                noteScale = noteScale,
                trackIndex = DynamicAction.mapTrackIndex(scale),
                blockRect = DynamicAction.calcBlockRect(noteScale, 3)
            )
        }
    }

    private var state: State by mutableStateOf(State.Ready) // 状态
    private var progress: Float by mutableFloatStateOf(0f) // 进度
    private var transferIndex by mutableIntStateOf(0) // 变轨信息
    private var actionResult by mutableStateOf<ActionResult?>(null) // 判定结果

    private var pressingAnimation = LineFrameAnimation(30, true) // 长按动画
    private var releasingAnimation = LineFrameAnimation(30) // 释放动画
    private var missingAnimation = LineFrameAnimation(30) // 消失动画

    override var bindId: Long? = null

    override val appearance: Long = start - (DynamicAction.BASE_DURATION * DynamicAction.HIT_RATIO.asVirtual).toLong()

    override val isDismiss: Boolean by derivedStateOf { state == State.Done }

    override fun onAdmission() { state = State.Moving }

    private fun ActionCallback.calcResult() {

    }

    override fun onUpdate(tick: Long, callback: ActionCallback) {
        when (state) {
            State.Ready, State.Done -> { } // 就绪或完成状态不更新
            State.Moving -> {
                // 更新进度
                progress = ((tick - appearance) / DynamicAction.BASE_DURATION.toFloat()).asActual.coerceIn(0f, 1f)
                // 超出死线仍未处理的音符标记错过
                if (progress > DynamicAction.deadline) {
                    state = State.Missing
                    missingAnimation.start()
                    callback.updateResult(ActionResult.MISS)
                }
            }
            State.Pressing -> {

            }
            State.Transfering -> {

            }
            State.Releasing -> {
                // 动画时间到，切换完成状态
                if (!releasingAnimation.update()) state = State.Done
            }
            State.Missing -> {
                progress = ((tick - appearance) / DynamicAction.BASE_DURATION.toFloat()).asActual.coerceIn(0f, 1f)
                if (!missingAnimation.update()) state = State.Done
            }
        }
    }

    override fun onTrackDown(track: Track, tick: Long, callback: ActionCallback): Boolean {
        val currentState = state
        val info = infos[0]
        // 校验轨道一致, 状态移动中
        if (track.index != info.trackIndex || currentState != State.Moving) return false

        return when {
            ActionResult.PERFECT.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.PERFECT
            ActionResult.GOOD.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.GOOD
            ActionResult.BAD.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.BAD
            ActionResult.MISS.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.MISS
            else -> null
        }?.also { result ->
            if (result == ActionResult.MISS) { // 错过
                state = State.Missing
                missingAnimation.start()
                callback.updateResult(result)
            }
            else { // 长按
                state = State.Pressing
                info.pressTick = tick
                actionResult = result
            }
        } != null
    }

    override fun onTrackUp(track: Track, tick: Long, callback: ActionCallback) {
        // 状态是按下或变轨, 并且已经记录按下时刻
        val currentState = state
        val info = infos[transferIndex]
        require(info.pressTick != null && currentState == State.Pressing || currentState == State.Transfering)
        // 立即结算
        info.releaseTick = tick
        state = State.Releasing
        releasingAnimation.start()
        callback.calcResult()
    }

    override fun onTrackTransfer(oldTrack: Track, newTrack: Track, tick: Long, callback: ActionCallback): Boolean {
        val currentState = state
        val info = infos[transferIndex]
        val nextTransferIndex = transferIndex + 1
        val nextInfo = infos.getOrNull(nextTransferIndex)
        // 状态是按下, 当前轨道已记录按下时刻
        require(info.pressTick != null && currentState == State.Pressing)

        info.releaseTick = tick
        // 所有轨道已经变换完成或变轨方向错误
        if (nextInfo == null || nextInfo.trackIndex != newTrack.index) {
            // 立即结算
            state = State.Releasing
            releasingAnimation.start()
            callback.calcResult()
            return false
        }
        // 开始变轨
        ++transferIndex
        return true
    }

    override fun onTrackMove(track: Track, offset: Float, tick: Long, callback: ActionCallback) {

    }

    override fun Drawer.onDraw(tracks: List<Track>, blockMap: ImageBitmap) {
        val currentState = state
        if (currentState == State.Ready || currentState == State.Done) return // 就绪或完成状态不渲染

        // 根据变轨信息找到当前轨道
        val info = infos.getOrNull(transferIndex)
        if (info != null) {
            val track = tracks[info.trackIndex]
            val (matrix, srcRect, _) = track.perspectiveMatrix
            val blockRect = info.blockRect

            transform({
                scale(progress, track.vertices)
                transform(matrix)
                if (track.isRight) scale(-1f, 1f, srcRect.center)
            }) {
                when (currentState) {
                    State.Moving -> {
                        image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect)
                    }
                    State.Pressing, State.Transfering -> {

                    }
                    State.Releasing -> {

                    }
                    State.Missing -> {
                        image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect, alpha = 1 - missingAnimation.progress)
                    }
                }
            }
        }
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
                    is RhymeAction.Note -> NoteAction(start, action) // 单音
                    is RhymeAction.Slur -> {
                        // 不同音级但同音高的仍然算做延音
                        val first = DynamicAction.mapTrackIndex(action.scale.first())
                        if (action.scale.all { first == DynamicAction.mapTrackIndex(it) }) FixedSlurAction(start, end, action) // 延音
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
                if (!action.isDismiss && block(action)) break
            }
        }
    }

    // 查找指定音符
    private fun findAction(pointerId: Long): DynamicAction? {
        if (actionIndex >= 0) {
            for (index in 0 .. actionIndex) {
                val action = queue[index]
                if (!action.isDismiss && action.bindId == pointerId) return action
            }
        }
        return null
    }

    // 音符行为回调
    private val callback = object : ActionCallback {
        override fun updateResult(result: ActionResult, scoreRatio: Float) {
            // 更新连击和分数
            val score = comboBoard.updateAction(result, scoreRatio)
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
        val activeTracks = trackMap.activeTracks
        return when (event) {
            is PointerDownEvent -> {
                // 按下时根据当前位置判定轨道
                val (track, inTracks) = trackMap.calcTrackIndex(event.position)
                if (track != null && inTracks) {
                    val trackIndex = track.index
                    // 防止多指按下统一轨道
                    if (activeTracks[trackIndex] == null) {
                        // 标记该轨道被按下
                        activeTracks[trackIndex] = event.id
                        // 寻找相匹配的音符并处理
                        foreachAction { action ->
                            if (action.bindId != null) false
                            else action.onTrackDown(track, compensateTick, callback).also {
                                if (it) action.bindId = event.id // 绑定指针ID
                            }
                        }
                    }
                }
                // 音符轨道事件区域本身就覆盖全屏, 并且位于事件触发最底层, 后续不需要再处理
                true
            }
            is PointerUpEvent -> {
                // 查找指针原始轨道
                val rawTrackIndex = activeTracks.indexOfFirst { it == event.id }
                if (rawTrackIndex != -1) {
                    val (track, inTracks) = trackMap.calcTrackIndex(event.position)
                    // 此时 event.position 一定位于当前轨道, 因为其他情况被移动排除了
                    require(inTracks && (track == null || track.index == rawTrackIndex))
                    val action = findAction(event.id)
                    if (action != null) {
                        action.onTrackUp(tracks[rawTrackIndex], compensateTick, callback)
                        // 防止意外被再次唤起按下
                        action.bindId = -1L
                    }
                    // 抬起时回调先于置空
                    activeTracks[rawTrackIndex] = null
                }
                true
            }
            is PointerMoveEvent -> {
                // 查找指针原始轨道
                val rawTrackIndex = activeTracks.indexOfFirst { it == event.id }
                if (rawTrackIndex != -1) {
                    val rawTrack = tracks[rawTrackIndex]
                    // 移动时根据当前位置判定轨道
                    val (track, inTracks) = trackMap.calcTrackIndex(event.position)
                    val action = findAction(event.id)
                    if (track != null) {
                        val trackIndex = track.index
                        if (rawTrackIndex != trackIndex) { // 轨道发生变化, 但绑定 ID 未发生变化无需处理
                            // 标记原始轨道被抬起
                            if (action != null) {
                                // 变轨
                                if (action.onTrackTransfer(rawTrack, track, compensateTick, callback)) {
                                    // 标记当前轨道被按下
                                    activeTracks[trackIndex] = event.id
                                }
                                else {
                                    // 不支持变轨则等价于被抬起
                                    action.onTrackUp(rawTrack, compensateTick, callback)
                                    action.bindId = -1L
                                }
                            }
                            activeTracks[rawTrackIndex] = null
                        }
                        else {
                            // 计算水平位置偏移
                            val offset = event.position.x - rawTrack.bottomCenter.x
                            // 在轨道内部移动
                            action?.onTrackMove(rawTrack, offset, compensateTick, callback)
                        }
                    }
                    else if (inTracks) {
                        // 计算水平位置偏移
                        val offset = event.position.x - rawTrack.bottomCenter.x
                        // 在轨道线交界处移动
                        action?.onTrackMove(rawTrack, offset, compensateTick, callback)
                    }
                    else { // 指针移出轨道区域则移除原始轨道
                        if (action != null) {
                            action.onTrackUp(rawTrack, compensateTick, callback)
                            action.bindId = -1L
                        }
                        activeTracks[rawTrackIndex] = null
                    }
                }
                true
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