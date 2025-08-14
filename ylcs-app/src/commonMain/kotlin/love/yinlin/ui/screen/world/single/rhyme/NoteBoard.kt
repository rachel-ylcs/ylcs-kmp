package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.util.fastForEach
import love.yinlin.common.Colors
import love.yinlin.data.music.Chorus
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.music.RhymeLine
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.onLine
import love.yinlin.extension.roundToIntOffset
import love.yinlin.extension.slope
import love.yinlin.extension.translate
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

// 轨道
@Stable
internal class Track(
    private val chorus: List<Chorus>
) : RhymeDynamic(), RhymeContainer.Rectangle {
    companion object {
        const val STROKE = 20f
        val Center = Offset(Size.Game.width / 2, 360f)
        val Tracks = arrayOf(
            Offset(0f, 0f),
            Offset(0f, Size.Game.height / 2),
            Offset(0f, Size.Game.height),
            Offset(Size.Game.width / 3, Size.Game.height),
            Offset(Size.Game.width * 2 / 3, Size.Game.height),
            Offset(Size.Game.width, Size.Game.height),
            Offset(Size.Game.width, Size.Game.height / 2),
            Offset(Size.Game.width, 0f),
        )
        val Slopes = Tracks.map { Center.slope(it) }
        val Shapes = List(7) { Path(arrayOf(Tracks[it], Tracks[it + 1], Center)) }

        fun calcTrackIndex(pos: Offset): Int? {
            val slope = Center.slope(pos)
            if (pos.x > Center.x) {
                for (i in 3 .. 6) {
                    if (slope >= Slopes[i + 1]) return i
                }
            }
            else {
                for (i in 3 downTo 0) {
                    if (slope <= Slopes[i]) return i
                }
            }
            return null
        }

        const val FPA = RhymeConfig.FPA * 2
    }

    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    private var currentTrackMap: Byte by mutableStateOf(0)
    fun getTrackMap(index: Int): Boolean = ((currentTrackMap.toInt() shr index) and 1) != 0
    inline fun safeSetTrackMap(index: Int, value: Boolean, block: () -> Unit) {
        val v = currentTrackMap.toInt()
        if ((((v shr index) and 1) != 0) == !value) {
            val mask = 1 shl index
            currentTrackMap = if (value) (v or mask).toByte() else (v and mask.inv()).toByte()
            block()
        }
    }

    private var chorusIndex = 0
    private var chorusMode by mutableStateOf(false)
    private var chorusFrame by mutableIntStateOf(0)
    private var chorusProgress by mutableFloatStateOf(0f)

    override fun onUpdate(position: Long) {
        if (chorusMode) {
            if (chorusFrame < FPA) chorusFrame++
            else chorusFrame = 0
        }
        else {
            if (chorusProgress > 0f) chorusProgress -= 1f / FPA
        }
        chorus.getOrNull(chorusIndex)?.let { chorus ->
            if (position >= chorus.start) {
                if (chorusMode) chorusProgress += 1000f / (chorus.end - chorus.start) / RhymeConfig.FPS
                else {
                    chorusMode = true
                    chorusFrame = 0
                    chorusProgress = 0f
                }
            }
            if (position > chorus.end && chorusMode) {
                chorusMode = false
                chorusFrame = 0
                ++chorusIndex
            }
        }
    }

    private fun DrawScope.drawTrackLine(start: Offset, end: Offset, stroke: Float) {
        // 阴影
        val shadowBase = chorusFrame * (1 - chorusFrame / FPA.toFloat()) / FPA
        val shadowWidth = 0.3f * shadowBase + 0.15f
        val shadowAlpha = 0.5f * shadowBase + 0.15f
        val shadowColor = if (chorusMode) Colors.Purple4 else Colors.Steel3
        repeat(5) {
            line(
                color = shadowColor,
                start = start,
                end = end,
                style = Stroke(width = stroke * (1.15f + it * shadowWidth), cap = StrokeCap.Round),
                alpha = shadowAlpha - (it * 0.03f)
            )
        }
        // 光带
        line(
            color = Colors.Steel4,
            start = start,
            end = end,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            alpha = 0.7f
        )
        // 高光
        line(
            color = Colors.White,
            start = start,
            end = end,
            style = Stroke(width = stroke * 0.8f, cap = StrokeCap.Round),
            alpha = 0.8f
        )
        // 副歌模式进度
        if (chorusProgress > 0f) {
            line(
                color = Colors.Steel4,
                start = start,
                end = start.onLine(end, chorusProgress),
                style = Stroke(width = stroke * 0.5f, cap = StrokeCap.Round),
                alpha = 0.8f
            )
        }
    }

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        // 画当前按下轨道高光
        repeat(7) {
            if (getTrackMap(it)) path(color = Colors.Steel3.copy(alpha = 0.2f), path = Shapes[it], style = Fill)
        }
        // 画轨道射线
        for (pos in Tracks) drawTrackLine(start = Center, end = pos, stroke = STROKE)
    }
}

// 提示区域
@Stable
private class TipArea : RhymeObject(), RhymeContainer.Rectangle {
    @Stable
    class Area(pos1: Offset, pos2: Offset, pos3: Offset, pos4: Offset) {
        val path: Path = Path(arrayOf(pos1, pos2, pos3, pos4))
        private val colorStops1 = arrayOf(
            0f to Colors.Steel3.copy(alpha = 0.8f),
            0.05f to Colors.Steel3.copy(alpha = 0.4f),
            0.1f to Colors.Steel3.copy(alpha = 0.1f),
            0.2f to Colors.Steel3.copy(alpha = 0.02f),
            1f to Colors.Transparent
        )
        // colorStops2 = 3 * colorStops1
        private val colorStops2 = arrayOf(
            0f to Colors.Steel3.copy(alpha = 0.8f),
            0.15f to Colors.Steel3.copy(alpha = 0.4f),
            0.3f to Colors.Steel3.copy(alpha = 0.1f),
            0.6f to Colors.Steel3.copy(alpha = 0.02f),
            1f to Colors.Transparent
        )
        private val isVertical = (pos3.y - pos1.y) > (pos3.x - pos1.x)
        private val startX = min(pos1.x, pos4.x)
        private val endX = max(pos2.x, pos3.x)
        private val startY = min(pos1.y, pos2.y)
        private val endY = max(pos3.y, pos4.y)
        private val horizontalBrush = if (isVertical) colorStops2 else colorStops1
        private val verticalBrush = if (isVertical) colorStops1 else colorStops2
        val brush1: Brush = Brush.horizontalGradient(*horizontalBrush, startX = startX, endX = endX)
        val brush2: Brush = Brush.horizontalGradient(*horizontalBrush, startX = endX, endX = startX)
        val brush3: Brush = Brush.verticalGradient(*verticalBrush, startY = startY, endY = endY)
        val brush4: Brush = Brush.verticalGradient(*verticalBrush, startY = endY, endY = startY)

        val center: Offset = run {
            // Cramer Rule
            val a1 = pos1.y - pos3.y
            val b1 = pos3.x - pos1.x
            val c1 = pos1.x * pos3.y - pos3.x * pos1.y
            val a2 = pos2.y - pos4.y
            val b2 = pos4.x - pos2.x
            val c2 = pos2.x * pos4.y - pos4.x * pos2.y
            val d = a1 * b2 - a2 * b1
            Offset((b1 * c2 - b2 * c1) / d, (a2 * c1 - a1 * c2) / d)
        }
        val angle: Float = atan2(Track.Center.x - center.x, center.y - Track.Center.y)
    }

    companion object {
        const val TIP_AREA_START = 0.8f
        const val TIP_AREA_END = 0.9f
        const val TIP_AREA_RANGE = TIP_AREA_END - TIP_AREA_START
        const val TIP_AREA_STROKE = 5f
    }

    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    private val points = arrayOf(
        Track.Center.onLine(Offset(0f, 0f), TIP_AREA_START),
        Track.Center.onLine(Offset(0f, 0f), TIP_AREA_END),
        Track.Center.onLine(Offset(0f, size.height / 2), TIP_AREA_END),
        Track.Center.onLine(Offset(0f, size.height / 2), TIP_AREA_START),
        Track.Center.onLine(Offset(0f, size.height), TIP_AREA_START),
        Track.Center.onLine(Offset(0f, size.height), TIP_AREA_END),
        Track.Center.onLine(Offset(size.width / 3, size.height), TIP_AREA_END),
        Track.Center.onLine(Offset(size.width / 3, size.height), TIP_AREA_START),
        Track.Center.onLine(Offset(size.width * 2 / 3, size.height), TIP_AREA_START),
        Track.Center.onLine(Offset(size.width * 2 / 3, size.height), TIP_AREA_END),
        Track.Center.onLine(Offset(size.width, size.height), TIP_AREA_END),
        Track.Center.onLine(Offset(size.width, size.height), TIP_AREA_START),
        Track.Center.onLine(Offset(size.width, size.height / 2), TIP_AREA_START),
        Track.Center.onLine(Offset(size.width, size.height / 2), TIP_AREA_END),
        Track.Center.onLine(Offset(size.width, 0f), TIP_AREA_END),
        Track.Center.onLine(Offset(size.width, 0f), TIP_AREA_START),
    )

    val areas = arrayOf(
        Area(points[1], points[0], points[3], points[2]),
        Area(points[2], points[3], points[4], points[5]),
        Area(points[4], points[7], points[6], points[5]),
        Area(points[7], points[8], points[9], points[6]),
        Area(points[8], points[11], points[10], points[9]),
        Area(points[12], points[13], points[10], points[11]),
        Area(points[15], points[14], points[13], points[12]),
    )

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        for (area in areas) {
            // 画提示区域线
            path(Colors.White, area.path, style = Stroke(width = TIP_AREA_STROKE))
            // 提示区域内发光遮罩
            path(area.brush1, area.path)
            path(area.brush2, area.path)
            path(area.brush3, area.path)
            path(area.brush4, area.path)
        }
    }
}

// 音符队列
@Stable
private class NoteQueue(
    lyrics: List<RhymeLine>,
    private val imageSet: ImageSet,
    private val track: Track,
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
        open fun onPointerDown(queue: NoteQueue, startTime: Long) { }
        open fun onPointerUp(queue: NoteQueue, isClick: Boolean, startTime: Long, endTime: Long) { }
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
                override val srcSize: Size,
                override val dstOffset: Offset
            ) : DrawImageData {
                override val srcOffsets = List(4) { srcOffset.translate(x = it * srcSize.width) }
                override val dstSize: Size = srcSize
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
                private const val PERFECT_RATIO = 0.25f
                private const val GOOD_RATIO = 0.5f
                private const val BAD_RATIO = 1f
                private const val MISS_RATIO = 3f

                private val ExtraNoteWidth = Track.Center.x * TipArea.TIP_AREA_RANGE
                private val ExtraNoteHeight = (Track.Tracks[2].y - Track.Center.y) * TipArea.TIP_AREA_RANGE
                private val TopNoteSize = Size(ExtraNoteWidth, Track.Tracks[1].y * (1 + TipArea.TIP_AREA_RANGE))
                private val BottomNoteSize = Size(ExtraNoteWidth, Track.Tracks[1].y + ExtraNoteHeight)
                private val LeftRightNoteSize = Size(ExtraNoteWidth + Track.Tracks[3].x, ExtraNoteHeight)
                private val CenterNoteSize = Size(Track.Tracks[3].x * (1 + TipArea.TIP_AREA_RANGE), ExtraNoteHeight)

                private val DrawMap = arrayOf(
                    DrawData(
                        srcOffset = Offset(TopNoteSize.width * 0, CenterNoteSize.height * 3),
                        srcSize = TopNoteSize,
                        dstOffset = Track.Center.onLine(Track.Tracks[0], 1 + TipArea.TIP_AREA_RANGE)
                    ),
                    DrawData(
                        srcOffset = Offset(TopNoteSize.width * 4, CenterNoteSize.height * 3),
                        srcSize = BottomNoteSize,
                        dstOffset = Track.Tracks[1].translate(x = -ExtraNoteWidth)
                    ),
                    DrawData(
                        srcOffset = Offset(0f, CenterNoteSize.height * 0),
                        srcSize = LeftRightNoteSize,
                        dstOffset = Track.Tracks[2].translate(x = -ExtraNoteWidth)
                    ),
                    DrawData(
                        srcOffset = Offset(0f, CenterNoteSize.height * 1),
                        srcSize = CenterNoteSize,
                        dstOffset = Track.Tracks[3].translate(x = -TipArea.TIP_AREA_RANGE * (Track.Center.x - Track.Tracks[3].x))
                    ),
                    DrawData(
                        srcOffset = Offset(0f, CenterNoteSize.height * 2),
                        srcSize = LeftRightNoteSize,
                        dstOffset = Track.Tracks[4]
                    ),
                    DrawData(
                        srcOffset = Offset(TopNoteSize.width * 12, CenterNoteSize.height * 3),
                        srcSize = BottomNoteSize,
                        dstOffset = Track.Tracks[6]
                    ),
                    DrawData(
                        srcOffset = Offset(TopNoteSize.width * 8, CenterNoteSize.height * 3),
                        srcSize = TopNoteSize,
                        dstOffset = Track.Tracks[7].translate(y = -TipArea.TIP_AREA_RANGE * Track.Center.y)
                    )
                )
            }

            // 单音符时长与实际字符发音时长无关, 全部为固定值
            override val duration: Long = (200 / TipArea.TIP_AREA_RANGE).toLong()
            override val appearance: Long = start - (duration * TipArea.TIP_AREA_START).toLong()

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
                    val dstOffset = Track.Center.onLine(drawData.dstOffset, progress).roundToIntOffset()
                    val dstSize = (drawData.dstSize * progress).roundToIntSize()
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
                }
            }

            override fun checkTrackIndex(index: Int): Boolean = index == trackIndex

            // 计算点击结果所占区间
            private fun calcResultRange(result: Float): LongRange {
                val ratio = TipArea.TIP_AREA_RANGE * result
                val startOffset = (duration * (TipArea.TIP_AREA_START - ratio)).toLong().coerceAtLeast(0L)
                val endOffset = (duration * (TipArea.TIP_AREA_START + ratio)).toLong().coerceAtMost(duration)
                return (appearance + startOffset) .. (appearance + endOffset)
            }

            override fun onPointerDown(queue: NoteQueue, startTime: Long) {
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
            override val duration: Long = 0L
            override val appearance: Long = 0L

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
        val trackIndex = Track.calcTrackIndex(pointer.position)
        if (trackIndex != null) pointer.handle(
            down = { // 按下
                // 防止多指按下同一个轨道
                track.safeSetTrackMap(trackIndex, true) {
                    foreachAction {
                        // 从队首遍历找到此轨道第一个正常音符
                        if (checkTrackIndex(trackIndex) && state == DynamicAction.State.Normal) {
                            onPointerDown(this@NoteQueue, pointer.startTime)
                            false
                        }
                        else true // 非正常音符或非对应轨道继续查找
                    }
                    // 轨道上无音符, 空点击
                }
            },
            up = { isClick, endTime -> // 抬起
                track.safeSetTrackMap(trackIndex, false) {
                    pointerMap[pointer.id]?.apply {
                        if (state == DynamicAction.State.Normal) {
                            onPointerUp(this@NoteQueue, isClick, pointer.startTime, endTime)
                        }
                    }
                }
            }
        )
        return trackIndex != null
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
    private val imageSet: ImageSet,
    tipArea: TipArea
) : RhymeDynamic(), RhymeContainer.Rectangle {
    @Stable
    private class TrackAnimation(val info: TrackAnimationInfo) {
        var action by mutableStateOf<NoteQueue.DynamicAction?>(null)
        var frame by mutableIntStateOf(0)
    }

    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    private val animations = tipArea.areas.mapIndexed { index, area ->
        TrackAnimation(TrackAnimationInfo(
            trackIndex = index,
            center = area.center,
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

    private val tipArea = TipArea()
    private val track = Track(lyrics.chorus)
    private val animations = Animations(imageSet, tipArea)
    private val noteQueue = NoteQueue(lyrics.lyrics, imageSet, track, animations, missEnvironment, scoreBoard, comboBoard)

    override fun onUpdate(position: Long) {
        track.onUpdate(position)
        noteQueue.onUpdate(position)
        animations.onUpdate(position)
    }

    override fun onEvent(pointer: Pointer): Boolean = noteQueue.onEvent(pointer)

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        tipArea.run { draw(textManager) }
        noteQueue.run { draw(textManager) }
        track.run { draw(textManager) }
        animations.run { draw(textManager) }
    }
}