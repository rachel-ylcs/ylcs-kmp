package love.yinlin.screen.world.single.rhyme.data

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.util.fastMapIndexed
import love.yinlin.compose.Colors
import love.yinlin.compose.Path
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.animation.LineFrameAnimation
import love.yinlin.compose.onCenter
import love.yinlin.compose.onLine
import love.yinlin.compose.translate
import love.yinlin.data.music.RhymeAction
import love.yinlin.screen.world.single.rhyme.RhymeAssets

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

    override fun Drawer.onDraw() {
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
                    val track = Tracks[trackIndex]
                    val progress = transferInfo.progress
                    val tailEndRatio = (progress.asVirtual - transferInfo.tailRatio).asActual
                    val startLeft = Tracks.Vertices.onLine(track.left, progress)
                    val startRight = Tracks.Vertices.onLine(track.right, progress)
                    val endLeft = Tracks.Vertices.onLine(track.left, tailEndRatio)
                    val endRight = Tracks.Vertices.onLine(track.right, tailEndRatio)

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
                        val p4 = p3.translate(x = 10f / p3.y * (Tracks.Vertices.x - p3.x), y = -10f)
                        path(color.copy(alpha = startAlpha), Path(arrayOf(p1, p2, p3, p4)))
                    }
                    lastInfo = ActionConnection(tailEndLeft, tailEndRight, track)
                }

                for (index in transferIndex .. transferInfos.lastIndex) {
                    val transferInfo = transferInfos[index]
                    val track = Tracks[transferInfo.trackIndex]
                    val (matrix, srcRect, _) = track.notePerspectiveMatrix
                    transform({
                        scale(transferInfo.progress, Tracks.Vertices)
                        transform(matrix)
                        if (track.isRight) scale(-1f, 1f, srcRect.center)
                    }) {
                        image(blockMap, if (track.isLeft) blockRect.first else blockRect.second, srcRect)
                    }
                }
            }
            is State.Releasing, State.Missing -> {
                for (transferInfo in transferInfos) {
                    val track = Tracks[transferInfo.trackIndex]
                    val (matrix, srcRect, _) = track.notePerspectiveMatrix
                    transform({
                        scale(transferInfo.progress, Tracks.Vertices)
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