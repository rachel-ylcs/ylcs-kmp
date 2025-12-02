package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import love.yinlin.compose.Colors
import love.yinlin.compose.Path
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Event
import love.yinlin.compose.game.traits.PointerDownEvent
import love.yinlin.compose.game.traits.PointerEvent
import love.yinlin.compose.game.traits.Transform
import love.yinlin.compose.onLine
import love.yinlin.compose.slope
import love.yinlin.compose.translate
import love.yinlin.screen.world.single.rhyme.RhymeConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager

// 轨道
@Stable
class Track(
    val index: Int, // 索引
    val scale: Int, // 音阶
    val vertices: Offset, // 顶点
    val left: Offset, // 左侧点
    val right: Offset, // 右侧点
) {
    companion object {
        // 音阶表
        val Scales = intArrayOf(4, 3, 2, 1, 5, 6, 7)
        // 顶点偏移比率
        const val VERTICES_TOP_RATIO = 0.2f
        // 虚拟顶点
        const val VIRTUAL_TOP_HEIGHT = VERTICES_TOP_RATIO * RhymeConfig.HEIGHT
        // 虚拟画布高度
        const val VIRTUAL_HEIGHT = RhymeConfig.HEIGHT + VIRTUAL_TOP_HEIGHT
        // 数量
        val Num = Scales.size
        // 轨道宽
        val maxWidth = RhymeConfig.WIDTH / Num
        // 线宽
        const val LINE_STROKE_WIDTH = 20f
        // 轨道背景色
        val BackgroundColor = Colors.Black.copy(alpha = 0.4f)
        // 轨道激活色
        val ActiveColor = Colors.Cyan3.copy(alpha = 0.2f)

        // 点击区域起始
        const val CLICK_START_RATIO = 0.8f
        // 点击区域结束
        const val CLICK_END_RATIO = 0.9f
        // 点击区域中间
        const val CLICK_CENTER_RATIO = (CLICK_START_RATIO + CLICK_END_RATIO) / 2
        // 点击区域区间
        const val CLICK_RANGE = CLICK_END_RATIO - CLICK_START_RATIO
    }

    // 位置
    val isLeft = index < Num / 2
    val isRight = index > (if (Num % 2 == 0) Num / 2 - 1 else Num / 2)
    val isCenter = index == (if (Num % 2 == 0) -1 else Num / 2)

    // 轨道线
    val leftLineLeft = left.translate(x = -LINE_STROKE_WIDTH / 2)
    val leftLineRight = left.translate(x = LINE_STROKE_WIDTH / 2)
    val leftLineArea = arrayOf(left.translate(x = -LINE_STROKE_WIDTH / 4), vertices, left.translate(x = LINE_STROKE_WIDTH / 4))
    val leftLineAreaPath = Path(leftLineArea)
    val leftLineShadowAreaPath = Path(arrayOf(leftLineLeft, vertices, leftLineRight))
    val rightLineLeft = right.translate(x = -LINE_STROKE_WIDTH / 2)
    val rightLineRight = right.translate(x = LINE_STROKE_WIDTH / 2)
    val rightLineArea = arrayOf(right.translate(x = -LINE_STROKE_WIDTH / 4), vertices, right.translate(x = LINE_STROKE_WIDTH / 4))
    val rightLineAreaPath = Path(rightLineArea)
    val rightLineShadowAreaPath = Path(arrayOf(rightLineLeft, vertices, rightLineRight))
    // 轨道线斜率范围
    val slopeLeft = vertices.slope(left)
    val slopeLeftRange: Pair<Float, Float> = vertices.slope(leftLineLeft) to vertices.slope(leftLineRight)
    val slopeRight = vertices.slope(right)
    val slopeRightRange: Pair<Float, Float> = vertices.slope(rightLineLeft) to vertices.slope(rightLineRight)
    // 轨道区域
    val area: Array<Offset> = arrayOf(vertices, left, right)
    val areaPath: Path = Path(area)
    // 透视矩阵
    val perspectiveMatrix = Drawer.calcFixedPerspectiveMatrix(3f, left, right, slopeLeft, slopeRight)
}

// 激活轨道
@Stable
class ActiveTrack {
    private val events = List<PointerEvent?>(Track.Num) { null }.toMutableStateList()

    operator fun get(index: Int): Boolean = events[index] != null

    fun safeSet(index: Int, event: PointerEvent) {
        // 防止多指按下同一个轨道
        if (event is PointerDownEvent) { // 按下
            if (events[index] == null) {
                events[index] = event
            }
        }
        else { // 抬起
            if (events.indexOfFirst { it?.id == event.id } == index) {
                events[index] = null
            }
        }
    }
}

@Stable
class TrackMap(
    rhymeManager: RhymeManager,
) : Spirit(rhymeManager), BoxBody {
    companion object {
        // 屏幕不可点击比率
        const val SCREEN_CLICK_AREA_RATIO = 0.7f
    }

    override val preTransform: List<Transform> = listOf(Transform.Translate(0f, -Track.VIRTUAL_TOP_HEIGHT))
    override val size: Size = Size(RhymeConfig.WIDTH, Track.VIRTUAL_HEIGHT)

    // 顶点
    val vertices = Offset(size.width / 2, 0f)

    // 轨道
    val tracks = buildList {
        var start = 0f
        repeat(Track.Num) { index ->
            val left = Offset(start, Track.VIRTUAL_HEIGHT)
            val right = left.translate(x = Track.maxWidth)
            add(Track(
                index = index,
                scale = Track.Scales[index],
                vertices = vertices,
                left = left,
                right = right
            ))
            start += Track.maxWidth
        }
    }

    // 轨道区域
    val tracksAreaPath = Path(arrayOf(
        tracks.first().left,
        vertices,
        tracks.last().right
    ))

    // 点击区域
    val clickArea = arrayOf(
        vertices.onLine(tracks.first().left, Track.CLICK_START_RATIO),
        vertices.onLine(tracks.first().left, Track.CLICK_END_RATIO),
        vertices.onLine(tracks.last().right, Track.CLICK_END_RATIO),
        vertices.onLine(tracks.last().right, Track.CLICK_START_RATIO),
    )

    // 轨道线画刷 (注意所有轨道左右侧线高度相同)
    val trackLineBrush = Brush.verticalGradient(
        colors = listOf(Colors.White, Colors.Transparent),
        startY = Track.VIRTUAL_HEIGHT,
        endY = 0f
    )

    // 当前按下轨道
    private val active = ActiveTrack()

    private fun calcTrackIndex(point: Offset): Track? {
        // 非屏幕可点击区域忽略
        if (point.y <= size.height * SCREEN_CLICK_AREA_RATIO) return null
        // 不需要计算点是否位于每个轨道三角形内，只需要计算斜率即可
        val slope = vertices.slope(point)
        if (slope >= 0f) { // 右侧
            for (i in 3 .. 6) {
                val (left, right)  = tracks[i].slopeRightRange
                if (slope >= right) {
                    // 点击到轨道线上的不计入
                    return if (slope >= left) tracks[i] else null
                }
            }
        }
        else { // 左侧
            for (i in 3 downTo 0) {
                val (left, right)  = tracks[i].slopeLeftRange
                if (slope <= left) {
                    return if (slope <= right) tracks[i] else null
                }
            }
        }
        return null
    }

    override fun onClientEvent(tick: Long, event: Event): Boolean = when (event) {
        is PointerEvent -> {
            // 获取指针所在轨道
            val track = calcTrackIndex(event.position)
            if (track != null) active.safeSet(track.index, event)
            track != null
        }
    }

    override fun Drawer.onClientDraw() {
        // 画轨道背景
        path(Track.BackgroundColor, tracksAreaPath)

        // 画点击区域横线
        line(Colors.Ghost, clickArea[0], clickArea[3], Stroke(5f), 0.6f)
        line(Colors.Ghost, clickArea[1], clickArea[2], Stroke(5f), 0.6f)

        for (index in 0 ..< Track.Num) {
            val track = tracks[index]
            // 画激活轨道
            if (active[index]) path(Track.ActiveColor, track.areaPath)

            // 画轨道左侧射线
            // 阴影
            path(Colors.Steel2, track.leftLineShadowAreaPath, alpha = 0.2f)
            // 轨道线用三角形模拟, 这样能做出一个越远越细的效果
            path(trackLineBrush, track.leftLineAreaPath)
            path(Colors.Steel4, track.leftLineAreaPath, 0.75f, Stroke(2f))
        }

        // 最后一个轨道右侧射线
        val track = tracks.last()
        path(Colors.Steel2, track.rightLineShadowAreaPath, alpha = 0.2f)
        path(trackLineBrush, track.rightLineAreaPath)
        path(Colors.Steel4, track.rightLineAreaPath, 0.75f, Stroke(2f))
    }
}