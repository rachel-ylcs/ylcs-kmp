package love.yinlin.screen.world.single.rhyme

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import love.yinlin.compose.*
import love.yinlin.data.music.Chorus
import kotlin.math.max
import kotlin.math.min

@Stable
internal class Track(
    val end: Offset // 轨道末端
) {
    companion object {
        // 轨道始端 (屏幕中心点)
        val Start = Offset(Size.Game.width / 2, 360f)

        // 所有轨道
        val Tracks = arrayOf(
            Track(Offset(0f, 0f)),
            Track(Offset(0f, Size.Game.height / 2)),
            Track(Offset(0f, Size.Game.height)),
            Track(Offset(Size.Game.width / 3, Size.Game.height)),
            Track(Offset(Size.Game.width * 2 / 3, Size.Game.height)),
            Track(Offset(Size.Game.width, Size.Game.height)),
            Track(Offset(Size.Game.width, Size.Game.height / 2)),
            Track(Offset(Size.Game.width, 0f)),
        )
    }

    // 轨道斜率
    val slope = Start.slope(end)
}

@Stable
internal class TrackArea(
    val index: Int,
    val scale: Int,
    tipPosIndex1: Int,
    tipPosIndex2: Int,
    tipPosIndex3: Int,
    tipPosIndex4: Int
) {
    companion object {
        // 提示区域起始
        const val TIP_START_RATIO = 0.8f
        // 提示区域结束
        const val TIP_END_RATIO = 0.9f
        // 提示区域区间
        const val TIP_RANGE = TIP_END_RATIO - TIP_START_RATIO

        private val TipPoints = arrayOf(
            Track.Start.onLine(Offset(0f, 0f), TIP_START_RATIO),
            Track.Start.onLine(Offset(0f, 0f), TIP_END_RATIO),
            Track.Start.onLine(Offset(0f, Size.Game.height / 2), TIP_END_RATIO),
            Track.Start.onLine(Offset(0f, Size.Game.height / 2), TIP_START_RATIO),
            Track.Start.onLine(Offset(0f, Size.Game.height), TIP_START_RATIO),
            Track.Start.onLine(Offset(0f, Size.Game.height), TIP_END_RATIO),
            Track.Start.onLine(Offset(Size.Game.width / 3, Size.Game.height), TIP_END_RATIO),
            Track.Start.onLine(Offset(Size.Game.width / 3, Size.Game.height), TIP_START_RATIO),
            Track.Start.onLine(Offset(Size.Game.width * 2 / 3, Size.Game.height), TIP_START_RATIO),
            Track.Start.onLine(Offset(Size.Game.width * 2 / 3, Size.Game.height), TIP_END_RATIO),
            Track.Start.onLine(Offset(Size.Game.width, Size.Game.height), TIP_END_RATIO),
            Track.Start.onLine(Offset(Size.Game.width, Size.Game.height), TIP_START_RATIO),
            Track.Start.onLine(Offset(Size.Game.width, Size.Game.height / 2), TIP_START_RATIO),
            Track.Start.onLine(Offset(Size.Game.width, Size.Game.height / 2), TIP_END_RATIO),
            Track.Start.onLine(Offset(Size.Game.width, 0f), TIP_END_RATIO),
            Track.Start.onLine(Offset(Size.Game.width, 0f), TIP_START_RATIO),
        )

        // 所有轨道区域
        val Areas = arrayOf(
            TrackArea(0, 6, 1, 0, 3, 2),
            TrackArea(1, 7, 2, 3, 4, 5),
            TrackArea(2, 1, 4, 7, 6, 5),
            TrackArea(3, 2, 7, 8, 9, 6),
            TrackArea(4, 3, 8, 11, 10, 9),
            TrackArea(5, 4, 12, 13, 10, 11),
            TrackArea(6, 5, 15, 14, 13, 12),
        )

        // 计算轨道
        fun calcIndex(pos: Offset): TrackArea? {
            val slope = Track.Start.slope(pos)
            if (pos.x > Track.Start.x) {
                for (i in 3 .. 6) {
                    if (slope >= Track.Tracks[i + 1].slope) return Areas[i]
                }
            }
            else {
                for (i in 3 downTo 0) {
                    if (slope <= Track.Tracks[i].slope) return Areas[i]
                }
            }
            return null
        }
    }

    // 提示区域点集
    val tipPos1 = TipPoints[tipPosIndex1]
    val tipPos2 = TipPoints[tipPosIndex2]
    val tipPos3 = TipPoints[tipPosIndex3]
    val tipPos4 = TipPoints[tipPosIndex4]
    // 是否是垂直方向
    val isVertical = (tipPos3.y - tipPos1.y) > (tipPos3.x - tipPos1.x)
    val isHorizontal = !isVertical
    // 轨道形状
    val shape = Path(arrayOf(Track.Tracks[index].end, Track.Tracks[index + 1].end, Track.Start))
    // 轨道末端中点
    val endCenter = Track.Tracks[index].end.onCenter(Track.Tracks[index + 1].end)
    // 提示区域形状
    val tipShape = Path(arrayOf(tipPos1, tipPos2, tipPos3, tipPos4))
    // 提示区域画刷
    val tipBrush = run {
        val colorStops1 = arrayOf(
            0f to Colors.Steel3.copy(alpha = 0.8f),
            0.05f to Colors.Steel3.copy(alpha = 0.4f),
            0.1f to Colors.Steel3.copy(alpha = 0.1f),
            0.2f to Colors.Steel3.copy(alpha = 0.02f),
            1f to Colors.Transparent
        )
        // colorStops2 = 3 * colorStops1
        val colorStops2 = arrayOf(
            0f to Colors.Steel3.copy(alpha = 0.8f),
            0.15f to Colors.Steel3.copy(alpha = 0.4f),
            0.3f to Colors.Steel3.copy(alpha = 0.1f),
            0.6f to Colors.Steel3.copy(alpha = 0.02f),
            1f to Colors.Transparent
        )
        val startX = min(tipPos1.x, tipPos4.x)
        val endX = max(tipPos2.x, tipPos3.x)
        val startY = min(tipPos1.y, tipPos2.y)
        val endY = max(tipPos3.y, tipPos4.y)
        val horizontalBrush = if (isHorizontal) colorStops1 else colorStops2
        val verticalBrush = if (isVertical) colorStops1 else colorStops2
        val brush1 = Brush.horizontalGradient(*horizontalBrush, startX = startX, endX = endX)
        val brush2 = Brush.horizontalGradient(*horizontalBrush, startX = endX, endX = startX)
        val brush3 = Brush.verticalGradient(*verticalBrush, startY = startY, endY = endY)
        val brush4 = Brush.verticalGradient(*verticalBrush, startY = endY, endY = startY)
        arrayOf(brush1, brush2, brush3, brush4)
    }
    // 提示区域中心
    val tipCenter = Offset.cramerCenter(tipPos1, tipPos2, tipPos3, tipPos4)
}

// 轨道
@Stable
internal class TrackMap(
    private val chorus: List<Chorus>
) : RhymeDynamic(), RhymeContainer.Rectangle {
    companion object {
        const val STROKE = 20f
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
            if (getTrackMap(it)) path(color = Colors.Steel3.copy(alpha = 0.2f), path = TrackArea.Areas[it].shape, style = Fill)
        }
        // 画轨道射线
        for (track in Track.Tracks) drawTrackLine(start = Track.Start, end = track.end, stroke = STROKE)
    }
}

// 提示区域
@Stable
internal class TipAreaMap : RhymeObject(), RhymeContainer.Rectangle {
    companion object {
        const val STROKE = 5f
    }

    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        for (area in TrackArea.Areas) {
            // 画提示区域线
            path(Colors.White, area.tipShape, style = Stroke(width = STROKE))
            // 提示区域内发光遮罩
            for (brush in area.tipBrush) path(brush, area.tipShape)
        }
    }
}