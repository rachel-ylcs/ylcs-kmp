package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.util.fastForEach
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.music.RhymeLine
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.onLine
import love.yinlin.extension.roundToIntOffset
import love.yinlin.extension.translate

// 音符队列
@Stable
private class NoteQueue(
    lyrics: List<RhymeLine>,
    private val imageSet: ImageSet,
    private val trackMap: TrackMap,
    private val animations: Animations,
    private val missEnvironment: MissEnvironment,
    private val scoreBoard: ScoreBoard,
    private val comboBoard: ComboBoard
) : RhymeDynamic(), RhymeContainer.Rectangle, RhymeEvent {
    @Stable
    sealed class DynamicAction {
        @Stable
        enum class State {
            Normal, Miss, Completed
        }

        abstract val action: RhymeAction
        abstract val duration: Long
        abstract val appearance: Long

        abstract fun update(position: Long)
        abstract fun DrawScope.draw(imageSet: ImageSet, position: Long)
        abstract fun checkTrackIndex(index: Int): Boolean
        open fun onPointerDown(queue: NoteQueue, currentTrackIndex: Int, startTime: Long) { }
        open fun onPointerUp(queue: NoteQueue, currentTrackIndex: Int, isClick: Boolean, startTime: Long, endTime: Long) { }
        open fun onDismiss(queue: NoteQueue) { }
        open fun isFiniteAnimation(): Boolean = false
        abstract fun DrawScope.drawAnimation(imageSet: ImageSet, info: TrackAnimationInfo, frame: Int)

        var state: State by mutableStateOf(State.Normal)
        var stateFrame: Int by mutableIntStateOf(0)

        @Stable
        class Note(start: Long, override val action: RhymeAction.Note) : DynamicAction() {
            @Stable
            private class DrawData(
                srcOffset: Offset,
                val srcSize: Size,
                val dstOffset: Offset
            ) {
                val srcOffsets = List(4) { srcOffset.translate(x = it * srcSize.width) }
                val dstSize: Size = srcSize
            }

            //                            单音生命周期
            // ------------------------------------------------------------------
            //    ↑     ↑    ↑     ↑      ↑        ↑      ↑     ↑     ↑      ↑
            //   出现  MISS  BAD  GOOD  PERFECT  PERFECT  GOOD  BAD  MISS    消失
            //                               发声
            // 字符的发声点是经过提示区域的开始
            // 提示区域的 PERFECT_RATIO 倍邻域为完美
            // 提示区域的 GOOD_RATIO 倍邻域为好
            // 提示区域的 BAD_RATIO 倍邻域为差
            // 提示区域的 MISS_RATIO 倍邻域为错过
            // 再往外的邻域不响应点击
            companion object {
                private const val DURATION_BASE = 200L
                private const val PERFECT_RATIO = 0.25f
                private const val GOOD_RATIO = 0.5f
                private const val BAD_RATIO = 1f
                private const val MISS_RATIO = 3f

                private val ExtraNoteWidth = Track.Start.x * TrackArea.TIP_RANGE
                private val ExtraNoteHeight = (Track.Tracks[2].end.y - Track.Start.y) * TrackArea.TIP_RANGE
                private val TopNoteSize = Size(ExtraNoteWidth, Track.Tracks[1].end.y * (1 + TrackArea.TIP_RANGE))
                private val BottomNoteSize = Size(ExtraNoteWidth, Track.Tracks[1].end.y + ExtraNoteHeight)
                private val LeftRightNoteSize = Size(ExtraNoteWidth + Track.Tracks[3].end.x, ExtraNoteHeight)
                private val CenterNoteSize = Size(Track.Tracks[3].end.x * (1 + TrackArea.TIP_RANGE), ExtraNoteHeight)

                private val DrawMap = arrayOf(
                    DrawData(
                        srcOffset = Offset(TopNoteSize.width * 0, CenterNoteSize.height * 3),
                        srcSize = TopNoteSize,
                        dstOffset = Track.Start.onLine(Track.Tracks[0].end, 1 + TrackArea.TIP_RANGE)
                    ),
                    DrawData(
                        srcOffset = Offset(TopNoteSize.width * 4, CenterNoteSize.height * 3),
                        srcSize = BottomNoteSize,
                        dstOffset = Track.Tracks[1].end.translate(x = -ExtraNoteWidth)
                    ),
                    DrawData(
                        srcOffset = Offset(0f, CenterNoteSize.height * 0),
                        srcSize = LeftRightNoteSize,
                        dstOffset = Track.Tracks[2].end.translate(x = -ExtraNoteWidth)
                    ),
                    DrawData(
                        srcOffset = Offset(0f, CenterNoteSize.height * 1),
                        srcSize = CenterNoteSize,
                        dstOffset = Track.Tracks[3].end.translate(x = -TrackArea.TIP_RANGE * (Track.Start.x - Track.Tracks[3].end.x))
                    ),
                    DrawData(
                        srcOffset = Offset(0f, CenterNoteSize.height * 2),
                        srcSize = LeftRightNoteSize,
                        dstOffset = Track.Tracks[4].end
                    ),
                    DrawData(
                        srcOffset = Offset(TopNoteSize.width * 12, CenterNoteSize.height * 3),
                        srcSize = BottomNoteSize,
                        dstOffset = Track.Tracks[6].end
                    ),
                    DrawData(
                        srcOffset = Offset(TopNoteSize.width * 8, CenterNoteSize.height * 3),
                        srcSize = TopNoteSize,
                        dstOffset = Track.Tracks[7].end.translate(y = -TrackArea.TIP_RANGE * Track.Start.y)
                    )
                )
            }

            // 单音符时长与实际字符发音时长无关, 全部为固定值
            override val duration: Long = (DURATION_BASE / TrackArea.TIP_RANGE).toLong()
            override val appearance: Long = start - (duration * TrackArea.TIP_START_RATIO).toLong()

            private val trackIndex = ((action.scale - 1) % 7 + 2) % 7
            private val trackLevel = (action.scale - 1) / 7 + 1

            override fun update(position: Long) {
                if (stateFrame < RhymeConfig.FPA) ++stateFrame
            }

            override fun DrawScope.draw(imageSet: ImageSet, position: Long) {
                DrawMap.getOrNull(trackIndex)?.let { drawData ->
                    val progress = ((position - appearance) / duration.toFloat()).coerceIn(0f, 1f)
                    val alpha = (stateFrame / RhymeConfig.FPA.toFloat()).coerceIn(0f, 1f)
                    val srcSize = drawData.srcSize.roundToIntSize()
                    val dstOffset = Track.Start.onLine(drawData.dstOffset, progress).roundToIntOffset()
                    val dstSize = (drawData.dstSize * progress).roundToIntSize()
                    // 画音符轨迹

                    // 画当前音符
                    drawImage(
                        image = imageSet.noteLayoutMap,
                        srcOffset = drawData.srcOffsets[trackLevel].roundToIntOffset(),
                        srcSize = srcSize,
                        dstOffset = dstOffset,
                        dstSize = dstSize,
                        alpha = if (state == State.Normal) alpha else (1 - alpha),
                        filterQuality = FilterQuality.High
                    )
                    if (state == State.Miss) {
                        // 画消失色音符
                        drawImage(
                            image = imageSet.noteLayoutMap,
                            srcOffset = drawData.srcOffsets[0].roundToIntOffset(),
                            srcSize = srcSize,
                            dstOffset = dstOffset,
                            dstSize = dstSize,
                            alpha = alpha,
                            filterQuality = FilterQuality.High
                        )
                    }
//                    val dstCenter = Track.Center.onLine(drawData.dstCenter, progress)
//                    drawRect(
//                        Colors.Red4,
//                        topLeft = dstCenter.translate(-10f, -10f),
//                        size = Size(20f, 20f)
//                    )
                }
            }

            override fun checkTrackIndex(index: Int): Boolean = index == trackIndex

            // 计算点击结果所占区间
            private fun calcResultRange(result: Float): LongRange {
                val ratio = TrackArea.TIP_RANGE * result
                val startOffset = (duration * (TrackArea.TIP_START_RATIO - ratio)).toLong().coerceAtLeast(0L)
                val endOffset = (duration * (TrackArea.TIP_START_RATIO + ratio)).toLong().coerceAtMost(duration)
                return (appearance + startOffset) .. (appearance + endOffset)
            }

            override fun onPointerDown(queue: NoteQueue, currentTrackIndex: Int, startTime: Long) {
                val result = when (startTime) {
                    in calcResultRange(PERFECT_RATIO) -> ComboBoard.ActionResult.PERFECT
                    in calcResultRange(GOOD_RATIO) -> ComboBoard.ActionResult.GOOD
                    in calcResultRange(BAD_RATIO) -> ComboBoard.ActionResult.BAD
                    in calcResultRange(MISS_RATIO) -> ComboBoard.ActionResult.MISS
                    else -> null
                }
                if (result != null) {
                    state = if (result == ComboBoard.ActionResult.MISS) State.Miss else State.Completed
                    stateFrame = 0
                    queue.animations.updateAnimation(trackIndex, this)
                    queue.updateResult(result)
                }
            }

            override fun onDismiss(queue: NoteQueue) {
                // 超出屏幕的音符将自动触发 MISS
                if (state == State.Normal) queue.updateResult(ComboBoard.ActionResult.MISS)
            }

            override fun DrawScope.drawAnimation(imageSet: ImageSet, info: TrackAnimationInfo, frame: Int) {
                drawImage(
                    image = imageSet.clickAnimationNote,
                    srcOffset = IntOffset(frame * 256, 0),
                    srcSize = IntSize(256, 256),
                    dstOffset = info.center.translate(x = -275f, y = -275f).roundToIntOffset(),
                    dstSize = IntSize(550, 550)
                )
            }
        }

        @Stable
        class FixedSlur(start: Long, end: Long, override val action: RhymeAction.Slur) : DynamicAction() {
            companion object {
                private const val DURATION_BASE_RATIO = 5L
                private const val LENGTH_RATIO = 0.7f
                private const val HEADER_RATIO = 0.1f
            }

            //                                                延音生命周期
            //    出现    ->    加首端    ->    加干路    ->    加尾端    ->    移动    ->    收干路    ->    收尾端    ->    消失
            //                 +2/3d          +4/3d          +2/3d         +4/3d         +2/3d         +1/3d
            //  appearance                                                               start          end
            //                 0%-10%         0%-60%         0%-70%        20%-90%      20%-90%       70%-90%
            override val duration: Long = (end - start) * DURATION_BASE_RATIO
            override val appearance: Long = start - duration * (DURATION_BASE_RATIO - 1) / DURATION_BASE_RATIO

            private val trackIndex = ((action.scale.first() - 1) % 7 + 2) % 7

            override fun update(position: Long) {

            }

            override fun DrawScope.draw(imageSet: ImageSet, position: Long) {

            }

            override fun checkTrackIndex(index: Int): Boolean = index == trackIndex

            override fun isFiniteAnimation(): Boolean = true

            override fun DrawScope.drawAnimation(imageSet: ImageSet, info: TrackAnimationInfo, frame: Int) {

            }
        }

        @Stable
        class OffsetSlur(start: Long, end: Long, override val action: RhymeAction.Slur) : DynamicAction() {
            override val duration: Long = 0L
            override val appearance: Long = 0L

            private val trackIndex = action.scale.map { ((it - 1) % 7 + 2) % 7 }

            override fun update(position: Long) {

            }

            override fun DrawScope.draw(imageSet: ImageSet, position: Long) {

            }

            override fun checkTrackIndex(index: Int): Boolean = index in trackIndex

            override fun isFiniteAnimation(): Boolean = true

            override fun DrawScope.drawAnimation(imageSet: ImageSet, info: TrackAnimationInfo, frame: Int) {

            }
        }
    }

    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    // 当前刻
    private var current by mutableLongStateOf(0)

    // 双指针维护基列表
    private var pushIndex by mutableIntStateOf(0)
    private var popIndex by mutableIntStateOf(0)

    // 预编译列表
    private val prebuildList = buildList {
        lyrics.fastForEach { line ->
            val theme = line.theme
            for (i in theme.indices) {
                val action = theme[i]
                val start = (theme.getOrNull(i - 1)?.end ?: 0) + line.start
                val end = action.end + line.start
                add(when (action) {
                    is RhymeAction.Note -> DynamicAction.Note(start, action) // 单音
                    is RhymeAction.Slur -> {
                        val first = action.scale.firstOrNull()
                        if (action.scale.all { it == first }) DynamicAction.FixedSlur(start, end, action) // 延音
                        else DynamicAction.OffsetSlur(start, end, action) // 连音
                    }
                })
            }
        }
    }

    // 指针表
    private val pointerMap = mutableMapOf<Long, DynamicAction>()

    private inline fun foreachAction(block: DynamicAction.() -> Boolean) {
        for (i in popIndex ..< pushIndex) {
            val dynAction = prebuildList[i]
            if (!dynAction.block()) break
        }
    }

    private fun updateResult(result: ComboBoard.ActionResult) {
        // 更新 MISS 环境
        if (result == ComboBoard.ActionResult.MISS) missEnvironment.stateFrame = MissEnvironment.FPA
        // 更新连击和分数
        val score = comboBoard.updateAction(result)
        scoreBoard.addScore(score)
    }

    override fun onUpdate(position: Long) {
        // 音符消失
        prebuildList.getOrNull(popIndex)?.let { dynAction ->
            // 到达消失刻
            if (position >= dynAction.appearance + dynAction.duration) {
                // 处理音符离开轨道事件
                dynAction.onDismiss(this)
                ++popIndex
            }
        }
        // 音符出现
        prebuildList.getOrNull(pushIndex)?.let { dynAction ->
            // 到达出现刻
            if (position >= dynAction.appearance) ++pushIndex
        }
        // 更新进度
        current = position
        // 更新音符
        foreachAction {
            update(position)
            true
        }
    }

    override fun onEvent(pointer: Pointer): Boolean {
        // 获取指针所在轨道
        val trackArea = TrackArea.calcIndex(pointer.position)
        if (trackArea != null) {
            val trackIndex = trackArea.index
            pointer.handle(
                down = { // 按下
                    // 防止多指按下同一个轨道
                    trackMap.safeSetTrackMap(trackIndex, true) {
                        foreachAction {
                            // 从队首遍历找到此轨道第一个正常音符
                            if (checkTrackIndex(trackIndex) && state == DynamicAction.State.Normal) {
                                onPointerDown(this@NoteQueue, trackIndex, pointer.startTime)
                                false
                            }
                            else true // 非正常音符或非对应轨道继续查找
                        }
                        // 轨道上无音符, 空点击
                    }
                },
                up = { isClick, endTime -> // 抬起
                    trackMap.safeSetTrackMap(trackIndex, false) {
                        pointerMap[pointer.id]?.apply {
                            if (state == DynamicAction.State.Normal) {
                                onPointerUp(this@NoteQueue, trackIndex, isClick, pointer.startTime, endTime)
                            }
                        }
                    }
                }
            )
        }
        return trackArea != null
    }

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        // 遍历已显示的音符队列
        foreachAction {
            draw(imageSet, current)
            true
        }
    }
}

@Stable
private data class TrackAnimationInfo(
    val trackIndex: Int,
    val center: Offset,
    val angle: Float
)

// 交互特效
@Stable
private class Animations(
    private val imageSet: ImageSet
) : RhymeDynamic(), RhymeContainer.Rectangle {
    @Stable
    private class TrackAnimation(val info: TrackAnimationInfo) {
        var action by mutableStateOf<NoteQueue.DynamicAction?>(null)
        var frame by mutableIntStateOf(0)
    }

    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    private val animations = TrackArea.Areas.mapIndexed { index, area ->
        TrackAnimation(TrackAnimationInfo(
            trackIndex = index,
            center = area.tipCenter,
            angle = area.angle
        ))
    }

    fun updateAnimation(trackIndex: Int, action: NoteQueue.DynamicAction?) {
        animations.getOrNull(trackIndex)?.let { animation ->
            animation.action = action
            animation.frame = 0
        }
    }

    override fun onUpdate(position: Long) {
        for (animation in animations) {
            animation.action?.let { action ->
                val frame = animation.frame
                animation.frame = if (frame < 16) frame + 1 else {
                    // 非持续性动画播放完毕后将重置动画帧
                    if (!action.isFiniteAnimation()) animation.action = null
                    0
                }
            }
        }
    }

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        for (animation in animations) {
            animation.action?.run { drawAnimation(imageSet, animation.info, animation.frame) }
        }
    }
}

// 音符板
@Stable
internal class NoteBoard(
    lyrics: RhymeLyricsConfig,
    imageSet: ImageSet,
    missEnvironment: MissEnvironment,
    scoreBoard: ScoreBoard,
    comboBoard: ComboBoard
) : RhymeDynamic(), RhymeContainer.Rectangle, RhymeEvent {
    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    private val tipAreaMap = TipAreaMap()
    private val trackMap = TrackMap(lyrics.chorus)
    private val animations = Animations(imageSet)
    private val noteQueue = NoteQueue(lyrics.lyrics, imageSet, trackMap, animations, missEnvironment, scoreBoard, comboBoard)

    override fun onUpdate(position: Long) {
        trackMap.onUpdate(position)
        noteQueue.onUpdate(position)
        animations.onUpdate(position)
    }

    override fun onEvent(pointer: Pointer): Boolean = noteQueue.onEvent(pointer)

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        tipAreaMap.run { draw(textManager) }
        noteQueue.run { draw(textManager) }
        trackMap.run { draw(textManager) }
        animations.run { draw(textManager) }
    }
}