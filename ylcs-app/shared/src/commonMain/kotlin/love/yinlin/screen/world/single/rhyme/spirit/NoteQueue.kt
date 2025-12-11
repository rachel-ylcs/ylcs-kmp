package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.util.fastMapIndexed
import love.yinlin.compose.Colors
import love.yinlin.compose.Path
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.animation.FrameAnimation
import love.yinlin.compose.game.animation.LineFrameAnimation
import love.yinlin.compose.game.animation.SpeedAdapter
import love.yinlin.compose.game.traits.*
import love.yinlin.compose.graphics.AnimatedWebp
import love.yinlin.compose.graphics.SolidColorFilter
import love.yinlin.compose.onCenter
import love.yinlin.compose.onLine
import love.yinlin.compose.translate
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeAssets
import love.yinlin.screen.world.single.rhyme.RhymeManager
import love.yinlin.screen.world.single.rhyme.RhymeSound

@Stable
interface ActionCallback {
    fun updateResult(result: ActionResult, scoreRatio: Float = 1f) // 处理音符结果
    fun playSound(type: RhymeSound) // 播放音效
}

@Stable
sealed interface DynamicAction {
    companion object {
        const val BASE_DURATION = 3000L // 音符持续时间
        const val BASE_DURATION_F = BASE_DURATION.toFloat()

        const val PERSPECTIVE_K = 3 // 透视参数
        const val HIT_RATIO = 0.8f // 判定线

        val deadline = ActionResult.BAD.endRange(HIT_RATIO) // 死线

        val BlockSize = Size(256f, 85f)
        fun calcBlockRect(scale: Int, base: Int): Pair<Rect, Rect> {
            val left = Rect(Offset(0f, (scale + base) * BlockSize.height), BlockSize)
            val center = Rect(Offset(BlockSize.width, (scale + base) * BlockSize.height), BlockSize)
            return left to center
        }

        fun mapTrackIndex(scale: Byte): Int = Track.Scales.indexOf((scale - 1) % 7 + 1)

        fun mapNoteScale(scale: Byte): Int = (scale - 1) / 7

        val ResultColorFilters = ActionResult.entries.map { SolidColorFilter(it.colors.first()) }
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
    fun Drawer.onDraw(tracks: List<Track>) // 渲染
}

private val Float.asUncheckedActual: Float get() = this / (DynamicAction.PERSPECTIVE_K + this * (1 - DynamicAction.PERSPECTIVE_K))
private val Float.asActual: Float get() = this.coerceIn(0f, 1f).asUncheckedActual.coerceIn(0f, 1f)
private val Float.asUncheckedVirtual: Float get() = DynamicAction.PERSPECTIVE_K * this / (1 + this * (DynamicAction.PERSPECTIVE_K - 1))
private val Float.asVirtual: Float get() = this.coerceIn(0f, 1f).asUncheckedVirtual.coerceIn(0f, 1f)

// 画平铺动画
private fun Drawer.drawPlainAnimation(
    track: Track,
    progress: Float,
    asset: AnimatedWebp,
    animation: FrameAnimation,
    scaleRatio: Float = 1f,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null
) {
    val plainRect = track.plainRect(progress, asset.width.toFloat() / asset.height, scaleRatio)
    drawAnimatedWebp(asset, animation.frame, plainRect, alpha, colorFilter)
}

// 画透视动画
private fun Drawer.drawPerspectiveAnimation(
    track: Track,
    srcRect: Rect,
    asset: AnimatedWebp,
    animation: FrameAnimation,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null
) {
    val frame = animation.frame.let {
        if (track.isCenter) it + animation.total else it
    }
    drawAnimatedWebp(asset, frame, srcRect, alpha, colorFilter)
}

@Stable
class NoteAction(
    assets: RhymeAssets,
    start: Long,
    override val action: RhymeAction.Note
) : DynamicAction {
    @Stable
    private sealed interface State {
        @Stable
        data object Ready : State // 就绪
        @Stable
        class Moving : State { // 移动
            var progress by mutableFloatStateOf(0f)
        }
        @Stable
        class Clicking(
            frameCount: Int,
            val result: ActionResult,
            val lastProgress: Float
        ) : State { // 点击中
            val animation = LineFrameAnimation(frameCount, adapter = SpeedAdapter(0.5f)).also { it.start() }
        }
        @Stable
        class Missing(frameCount: Int, lastProgress: Float) : State { // 错过中
            var progress by mutableFloatStateOf(lastProgress)
            val animation = LineFrameAnimation(frameCount / 2, adapter = SpeedAdapter(0.5f)).also { it.start() }
        }
        @Stable
        data object Done : State // 已完成
    }

    companion object {
        val ColorFilters = arrayOf(
            SolidColorFilter(Colors.Cyan3),
            SolidColorFilter(Colors.Green3),
            SolidColorFilter(Colors.Purple3),
        )
    }

    private val blockMap = assets.blockMap()
    private val noteClick = assets.noteClick()
    private val noteDismiss = assets.noteDismiss()
    private val soundNoteClick = assets.soundNoteClick

    private val trackIndex = DynamicAction.mapTrackIndex(action.scale)
    private val noteScale = DynamicAction.mapNoteScale(action.scale)
    private val blockRect = DynamicAction.calcBlockRect(noteScale, 0)

    private var state: State by mutableStateOf(State.Ready) // 状态

    override var bindId: Long? = null

    // A[(start - appearance) / duration] = hit
    // -> (start - appearance) / duration = V(hit)
    // -> start - appearance = duration * V(hit)
    // -> appearance = start - duration * V(hit)
    override val appearance: Long = start - (DynamicAction.BASE_DURATION * DynamicAction.HIT_RATIO.asUncheckedVirtual).toLong()

    override val isDismiss: Boolean by derivedStateOf { state == State.Done }

    override fun onAdmission() { state = State.Moving() }

    override fun onUpdate(tick: Long, callback: ActionCallback) {
        when (val currentState = state) {
            State.Ready, State.Done -> { } // 就绪或完成状态不更新
            is State.Moving -> {
                // 更新进度
                val progress = ((tick - appearance) / DynamicAction.BASE_DURATION_F).asActual
                currentState.progress = progress
                // 超出死线仍未处理的音符标记错过
                if (progress > DynamicAction.deadline) {
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
                currentState.progress = ((tick - appearance) / DynamicAction.BASE_DURATION_F).asUncheckedActual.coerceAtMost(0.95f)
            }
        }
    }

    override fun onTrackDown(track: Track, tick: Long, callback: ActionCallback): Boolean {
        // 校验轨道一致, 状态移动中
        val currentState = state
        if (track.index != trackIndex || currentState !is State.Moving) return false
        val progress = currentState.progress
        return ActionResult.inRange(DynamicAction.HIT_RATIO, progress)?.also { result ->
            // 切换点击态或错过态
            state = if (result == ActionResult.MISS) State.Missing(noteDismiss.frameCount, progress)
                else State.Clicking(noteClick.frameCount, result, progress)
            callback.updateResult(result)
            callback.playSound(soundNoteClick)
        } != null
    }

    override fun onTrackUp(track: Track, tick: Long, callback: ActionCallback) { }

    override fun onTrackTransfer(oldTrack: Track, newTrack: Track, tick: Long, callback: ActionCallback): Boolean = false

    override fun Drawer.onDraw(tracks: List<Track>) {
        val currentState = state
        if (currentState == State.Ready || currentState == State.Done) return // 就绪或完成状态不渲染

        val track = tracks[trackIndex]
        val (matrix, srcRect, _) = track.perspectiveMatrix
        val imgRect = if (track.isCenter) blockRect.second else blockRect.first

        when (currentState) {
            is State.Moving -> {
                transform({
                    // 先将轨道底部的画布以顶点为中心缩放到指定进度上
                    scale(currentState.progress, track.vertices)
                    // 然后透视
                    transform(matrix)
                    // 再根据轨道左右位置决定是否水平翻转
                    if (track.isRight) flipX(srcRect.center)
                }) {
                    image(blockMap, imgRect, srcRect)
                }
            }
            is State.Clicking -> {
                val lastProgress = currentState.lastProgress
                transform({
                    scale(lastProgress, track.vertices)
                    transform(matrix)
                    if (track.isRight) flipX(srcRect.center)
                }) {
                    image(blockMap, imgRect, srcRect, alpha = (1 - currentState.animation.progress * 1.5f).coerceAtLeast(0f))
                }
                drawPlainAnimation(track, lastProgress, noteClick, currentState.animation, colorFilter = DynamicAction.ResultColorFilters[currentState.result.ordinal])
            }
            is State.Missing -> {
                transform({
                    scale(currentState.progress, track.vertices)
                    transform(matrix)
                    if (track.isRight) flipX(srcRect.center)
                }) {
                    image(blockMap, imgRect, srcRect, alpha = (1 - currentState.animation.progress * 2f).coerceAtLeast(0f))
                    drawPerspectiveAnimation(track, srcRect, noteDismiss, currentState.animation, colorFilter = ColorFilters[noteScale])
                }
            }
        }
    }
}

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

    companion object {
        val ColorFilters = arrayOf(
            SolidColorFilter(Colors.Red5),
            SolidColorFilter(Colors.Orange6),
            SolidColorFilter(Colors.Cyan6),
        )
        val TailBrushes = arrayOf(
            listOf(Colors.Red5.copy(alpha = 0.8f), Colors.Transparent),
            listOf(Colors.Orange6.copy(alpha = 0.8f), Colors.Transparent),
            listOf(Colors.Cyan6.copy(alpha = 0.8f), Colors.Transparent)
        )
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
            val headLeft = track.vertices.onLine(track.left, headProgress)
            val headRight = track.vertices.onLine(track.right, headProgress)
            val tailLeft = track.vertices.onLine(track.left, tailProgress)
            val tailRight = track.vertices.onLine(track.right, tailProgress)
            path(
                brush = Brush.verticalGradient(TailBrushes[noteScale], headLeft.y, tailLeft.y),
                // 尾部起始连线的六等分点
                path = Path(arrayOf(
                    headLeft.onLine(headRight, 0.166667f),
                    tailLeft.onLine(tailRight, 0.166667f),
                    tailLeft.onLine(tailRight, 0.833333f),
                    headLeft.onLine(headRight, 0.833333f)
                ))
            )
        }
    }

    override fun Drawer.onDraw(tracks: List<Track>) {
        val currentState = state
        if (currentState == State.Ready || currentState == State.Done) return // 就绪或完成状态不渲染

        val track = tracks[trackIndex]
        val (matrix, srcRect, _) = track.perspectiveMatrix
        val imgRect = if (track.isCenter) blockRect.second else blockRect.first

        when (currentState) {
            is State.Moving -> {
                // 拖尾
                val headProgress = currentState.headProgress
                drawTrailing(track, headProgress, currentState.tailProgress)
                // 按键
                transform({
                    scale(headProgress, track.vertices)
                    transform(matrix)
                    if (track.isRight) flipX(srcRect.center)
                }) {
                    image(blockMap, imgRect, srcRect)
                }
            }
            is State.Pressing -> {
                // 拖尾
                val headProgress = currentState.lastHeadProgress
                drawTrailing(track, headProgress, currentState.tailProgress)
                // 动画
                drawPlainAnimation(track, DynamicAction.HIT_RATIO, longPress, currentState.animation, scaleRatio = 1.5f, colorFilter = ColorFilters[noteScale])
            }
            is State.Releasing -> { } // 暂定为空
            is State.Missing -> {
                // 拖尾
                val headProgress = currentState.headProgress
                val tailProgress = currentState.tailProgress
                drawTrailing(track, headProgress, tailProgress)

                // 按键
                transform({
                    scale(headProgress, track.vertices)
                    transform(matrix)
                    if (track.isRight) flipX(srcRect.center)
                }) {
                    image(blockMap, imgRect, srcRect, alpha = (1 - currentState.animation.progress * 1.5f).coerceAtLeast(0f))
                    drawPerspectiveAnimation(track, srcRect, noteDismiss, currentState.animation, colorFilter = ColorFilters[noteScale])
                }
            }
        }
    }
}

@Stable
class OffsetSlurAction(
    assets: RhymeAssets,
    private val start: Long,
    private val end: Long,
    override val action: RhymeAction.Slur
) : DynamicAction {
    // 长按信息
    @Stable
    data class PressInfo(
        val pressTick: Long, // 按下时间
        val releaseTick: Long?, // 释放时间
        val result: ActionResult // 按下结果
    )

    // 变轨信息
    @Stable
    private class TransferInfo(
        val tick: Long, // 按下时刻
        val duration: Long, // 时长
        val trackIndex: Int, // 轨道索引
    ) {
        val appearance: Long = tick - (DynamicAction.BASE_DURATION * DynamicAction.HIT_RATIO.asVirtual).toLong()

        var progress: Float by mutableFloatStateOf(0f) // 进度
        var tailRatio: Float by mutableFloatStateOf(duration / DynamicAction.BASE_DURATION.toFloat())

        val missingAnimation = LineFrameAnimation(30) // 消失动画
        val releasingAnimation = LineFrameAnimation(30) // 释放动画
    }

    // 连接信息
    @Stable
    private data class ActionConnection(
        val left: Offset,
        val right: Offset,
        val track: Track
    )

    @Stable
    private sealed interface State {
        @Stable
        data object Ready : State // 就绪
        @Stable
        data object Moving : State // 移动
        @Stable
        data class Pressing(
            val infos: List<PressInfo>, // 按下信息
            val transferIndex: Int, // 变轨索引
        ) : State // 长按中
        @Stable
        data class Releasing(
            val infos: List<PressInfo>, // 按下信息
            val transferIndex: Int, // 变轨索引
        ) : State // 释放中
        @Stable
        data object Missing : State // 错过中
        @Stable
        data object Done : State // 已完成
    }

    private val blockMap = assets.blockMap()

    // 长按总时长
    private val pressDuration = (end - start)
    // 每个音符长按时长
    private val perDuration = pressDuration / action.scale.size
    // 原始变轨信息
    private val transferInfos = action.scale.fastMapIndexed { index, scale ->
        TransferInfo(
            tick = start + perDuration * index,
            duration = perDuration,
            trackIndex = DynamicAction.mapTrackIndex(scale),
        )
    }
    private val blockRect = DynamicAction.calcBlockRect(0, 6)

    private var state: State by mutableStateOf(State.Ready) // 状态

    private var pressingAnimation = LineFrameAnimation(30, true) // 长按动画

    override var bindId: Long? = null

    override val appearance: Long = start - (DynamicAction.BASE_DURATION * DynamicAction.HIT_RATIO.asVirtual).toLong()

    override val isDismiss: Boolean by derivedStateOf { state == State.Done }

    override fun onAdmission() { state = State.Moving }

    private fun ActionCallback.calcResult() {

    }

    override fun onUpdate(tick: Long, callback: ActionCallback) {
        when (val currentState = state) {
            State.Ready, State.Done -> { } // 就绪或完成状态不更新
            State.Moving -> {
                // 更新进度
                for (transferInfo in transferInfos) {
                    transferInfo.progress = ((tick - transferInfo.appearance) / DynamicAction.BASE_DURATION.toFloat()).asActual
                }
                // 超出死线仍未处理的音符标记错过
                if (transferInfos[0].progress > DynamicAction.deadline) {
                    state = State.Missing
                    for (transferInfo in transferInfos) transferInfo.missingAnimation.start()
                    callback.updateResult(ActionResult.MISS)
                }
            }
            is State.Pressing -> {
                pressingAnimation.update()
            }
            is State.Releasing -> {
                // 动画时间到，切换完成状态
                val transferIndex = currentState.transferIndex
                for (index in transferIndex .. transferInfos.lastIndex) {
                    if (!transferInfos[index].releasingAnimation.update()) {
                        state = State.Done
                        break
                    }
                }
            }
            State.Missing -> {
                for (transferInfo in transferInfos) {
                    transferInfo.progress = ((tick - transferInfo.appearance) / DynamicAction.BASE_DURATION.toFloat()).asActual.coerceIn(0f, 1f)
                    if (!transferInfo.missingAnimation.update()) {
                        state = State.Done
                        break
                    }
                }
            }
        }
    }

    override fun onTrackDown(track: Track, tick: Long, callback: ActionCallback): Boolean {
        // 校验轨道一致, 状态移动中
        val transferInfo = transferInfos[0]
        if (track.index != transferInfo.trackIndex || state != State.Moving) return false
        val progress = transferInfo.progress

        return when {
            ActionResult.PERFECT.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.PERFECT
            ActionResult.GOOD.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.GOOD
            ActionResult.BAD.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.BAD
            ActionResult.MISS.inRange(DynamicAction.HIT_RATIO, progress) -> ActionResult.MISS
            else -> null
        }?.also { result ->
            if (result == ActionResult.MISS) { // 错过
                state = State.Missing
                for (transferInfo in transferInfos) transferInfo.missingAnimation.start()
                callback.updateResult(result)
            }
            else { // 长按
                state = State.Pressing(
                    infos = listOf(
                        PressInfo(
                            pressTick = tick,
                            releaseTick = null,
                            result = result
                        )
                    ),
                    transferIndex = 0
                )
                pressingAnimation.start()
            }
        } != null
    }

    override fun onTrackUp(track: Track, tick: Long, callback: ActionCallback) {
        // 状态变轨
        val currentState = state
        if (currentState is State.Pressing) {
            // 立即结算
            val infos = currentState.infos.toMutableList()
            val info = infos.last().copy(releaseTick = tick)
            infos.removeLast()
            infos.add(info)
            val transferIndex = currentState.transferIndex
            state = State.Releasing(infos, transferIndex)
            for (index in transferIndex .. transferInfos.lastIndex) {
                transferInfos[index].releasingAnimation.start()
            }
            callback.calcResult()
        }
    }

    override fun onTrackTransfer(oldTrack: Track, newTrack: Track, tick: Long, callback: ActionCallback): Boolean {
        // 状态变轨
        if (state is State.Pressing) {
//            val nextTransferIndex = transferIndex + 1
//            val nextInfo = infos[nextTransferIndex]
//            // 如果变轨序号等于下一个变轨序号
//            if (nextInfo.trackIndex == newTrack.index) {
//                // 开始变轨
//                transferIndex = nextTransferIndex
//                nextInfo.pressTick = tick
//                // 如果已经完成最后一个轨道变换立即结算
//                if (nextTransferIndex == infos.lastIndex) {
//                    state = State.Releasing
//                    releasingAnimation.start()
//                    callback.calcResult()
//                }
//            }
            return true
        }
        return false
    }

    override fun Drawer.onDraw(tracks: List<Track>) {
        val currentState = state
        if (currentState == State.Ready || currentState == State.Done) return // 就绪或完成状态不渲染

        when (currentState) {
            State.Moving, is State.Pressing -> {
                val transferIndex = if (currentState is State.Pressing) currentState.transferIndex else 0
                val perAlpha = 0.8f / transferInfos.size
                var lastInfo: ActionConnection? = null

                // 拖尾
                for (index in transferIndex .. transferInfos.lastIndex) {
                    val transferInfo = transferInfos[index]
                    val trackIndex = transferInfo.trackIndex
                    val track = tracks[trackIndex]
                    val progress = transferInfo.progress
                    val tailEndRatio = (progress.asVirtual - transferInfo.tailRatio).asActual
                    val startLeft = track.vertices.onLine(track.left, progress)
                    val startRight = track.vertices.onLine(track.right, progress)
                    val endLeft = track.vertices.onLine(track.left, tailEndRatio)
                    val endRight = track.vertices.onLine(track.right, tailEndRatio)

                    val color = Colors.Pink3
                    val startAlpha = 0.8f - index * perAlpha
                    val endAlpha = (startAlpha - perAlpha).coerceAtLeast(0f)

                    // 尾部起始连线的六等分点
                    val tailEndLeft = endLeft.onLine(endRight, 0.166667f)
                    val tailEndRight = endLeft.onLine(endRight, 0.833333f)
                    path(
                        brush = Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = startAlpha), color.copy(alpha = endAlpha)),
                            startY = startLeft.y,
                            endY = endLeft.y),
                        path = Path(arrayOf(
                            startLeft.onLine(startRight, 0.166667f),
                            tailEndLeft,
                            tailEndRight,
                            startLeft.onLine(startRight, 0.833333f)
                        ))
                    )

                    // 链接
                    lastInfo?.let { (lastLeft, lastRight, track) ->
                        val p2 = startLeft.onCenter(startRight)
                        val p1 = p2.translate(y = -10f)
                        val p3 = if (trackIndex > track.index) lastLeft else lastRight
                        val p4 = p3.translate(x = 10f / p3.y * (track.vertices.x - p3.x), y = -10f)
                        path(color.copy(alpha = startAlpha), Path(arrayOf(p1, p2, p3, p4)))
                    }
                    lastInfo = ActionConnection(tailEndLeft, tailEndRight, track)
                }

                for (index in transferIndex .. transferInfos.lastIndex) {
                    val transferInfo = transferInfos[index]
                    val track = tracks[transferInfo.trackIndex]
                    val (matrix, srcRect, _) = track.perspectiveMatrix
                    transform({
                        scale(transferInfo.progress, track.vertices)
                        transform(matrix)
                        if (track.isRight) scale(-1f, 1f, srcRect.center)
                    }) {
                        image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect)
                    }
                }
            }
            is State.Releasing, State.Missing -> {
                for (transferInfo in transferInfos) {
                    val track = tracks[transferInfo.trackIndex]
                    val (matrix, srcRect, _) = track.perspectiveMatrix
                    transform({
                        scale(transferInfo.progress, track.vertices)
                        transform(matrix)
                        if (track.isRight) scale(-1f, 1f, srcRect.center)
                    }) {
                        val progress = when (currentState) {
                            is State.Releasing -> transferInfo.releasingAnimation.progress
                            is State.Missing -> transferInfo.missingAnimation.progress
                        }
                        image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect, alpha = 1 - progress)
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

    // 预编译队列
    private val queue: List<DynamicAction> = buildList(lyrics.size) {
        for (line in lyrics) {
            val theme = line.theme
            for (i in theme.indices) {
                val action = theme[i]
                val start = (theme.getOrNull(i - 1)?.end ?: 0) + line.start
                val end = action.end + line.start
                val dynamicAction = when (action) {
                    is RhymeAction.Note -> NoteAction(rhymeManager.assets, start, action) // 单音
                    is RhymeAction.Slur -> {
                        // 不同音级但同音高的仍然算做延音
                        val first = DynamicAction.mapTrackIndex(action.scale.first())
                        if (action.scale.all { first == DynamicAction.mapTrackIndex(it) }) FixedSlurAction(rhymeManager.assets, start, end, action) // 延音
                        else OffsetSlurAction(rhymeManager.assets, start, end, action) // 连音
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

        override fun playSound(type: RhymeSound) = rhymeManager.playSound(type)
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
                    }
                    else if (!inTracks) { // 指针移出轨道区域则移除原始轨道
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
            it.apply { onDraw(tracks) }
            false
        }
    }
}