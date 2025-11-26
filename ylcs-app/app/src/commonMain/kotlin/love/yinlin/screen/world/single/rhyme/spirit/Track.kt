package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import love.yinlin.compose.Colors
import love.yinlin.compose.Path
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.Pointer
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.PointerTrigger
import love.yinlin.compose.game.traits.Transform
import love.yinlin.compose.game.traits.Visible
import love.yinlin.compose.onLine
import love.yinlin.compose.slope
import love.yinlin.compose.translate
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager

// 可点击区域
@Stable
class TrackClickArea(val area: Array<Offset>) {
    val areaPath: Path = Path(area)
    val perspectiveMatrix: Pair<Matrix, Rect> = Drawer.calcBottomPerspectiveMatrix(area)
    val brush = Brush.radialGradient(
        *arrayOf(
            0f to Colors.Transparent,
            0.3f to Colors.Steel3.copy(alpha = 0.1f),
            0.5f to Colors.Steel3.copy(alpha = 0.2f),
            0.75f to Colors.Steel3.copy(alpha = 0.4f),
            1f to Colors.Steel3.copy(alpha = 0.8f)
        ),
        center = perspectiveMatrix.second.center,
        radius = perspectiveMatrix.second.width * 0.707f
    )
}

// 轨道
@Stable
data class Track(
    val index: Int, // 索引
    val scale: Int, // 音阶
    val vertices: Offset, // 顶点
    val left: Offset, // 左侧点
    val right: Offset, // 右侧点
) {
    companion object {
        // 音阶表
        val Scales = arrayOf(4, 3, 2, 1, 5, 6, 7)
        // 顶点偏移比率
        const val VERTICES_TOP_RATIO = 0.2f
        // 数量
        val Num = Scales.size
        // 线宽
        const val LINE_STROKE_WIDTH = 20f
        // 屏幕可点击比率
        const val SCREEN_CLICK_AREA_RATIO = 0.6667f
        // 点击区域起始
        const val TIP_START_RATIO = 0.8f
        // 点击区域结束
        const val TIP_END_RATIO = 0.9f
        // 点击区域区间
        const val TIP_RANGE = TIP_END_RATIO - TIP_START_RATIO
    }

    // 左侧点斜率范围
    val slopeLeftRange: Pair<Float, Float> = vertices.slope(left.translate(x = -LINE_STROKE_WIDTH / 2)) to vertices.slope(left.translate(x = LINE_STROKE_WIDTH / 2))
    // 右侧点斜率范围
    val slopeRightRange: Pair<Float, Float> = vertices.slope(right.translate(x = -LINE_STROKE_WIDTH / 2)) to vertices.slope(right.translate(x = LINE_STROKE_WIDTH / 2))
    val area: Array<Offset> = arrayOf(vertices, left, right) // 轨道区域
    val areaPath: Path = Path(area) // 轨道区域路径
    // 点击区域
    val clickArea = TrackClickArea(arrayOf(
        vertices.onLine(left, (TIP_START_RATIO + VERTICES_TOP_RATIO) / (1 + VERTICES_TOP_RATIO)),
        vertices.onLine(left, (TIP_END_RATIO + VERTICES_TOP_RATIO) / (1 + VERTICES_TOP_RATIO)),
        vertices.onLine(right, (TIP_END_RATIO + VERTICES_TOP_RATIO) / (1 + VERTICES_TOP_RATIO)),
        vertices.onLine(right, (TIP_START_RATIO + VERTICES_TOP_RATIO) / (1 + VERTICES_TOP_RATIO))
    ))
}

@Stable
class TrackUI(
    rhymeManager: RhymeManager,
    lyrics: RhymeLyricsConfig,
) : Spirit(rhymeManager), BoxBody, Visible, PointerTrigger {
    override val preTransform: List<Transform> = listOf(Transform.Translate(0f, -1080f * Track.VERTICES_TOP_RATIO))
    override val size: Size = Size(1920f, 1080f * (1 + Track.VERTICES_TOP_RATIO))

    // 顶点
    private val vertices = Offset(size.width / 2, 0f)

    // 轨道
    private val tracks = buildList {
        val trackWidth = this@TrackUI.size.width / Track.Num
        val bottom = this@TrackUI.size.height
        var start = 0f
        repeat(Track.Num) { index ->
            val left = Offset(start, bottom)
            val right = Offset(start + trackWidth, bottom)
            add(Track(
                index = index,
                scale = Track.Scales[index],
                vertices = vertices,
                left = left,
                right = right
            ))
            start += trackWidth
        }
    }

    // 当前按下轨道
    private var currentTrackMap: Byte by mutableStateOf(0)
    fun getTrackMap(index: Int): Boolean = ((currentTrackMap.toInt() shr index) and 1) != 0
    fun setTrackMap(index: Int, value: Boolean) {
        val v = currentTrackMap.toInt()
        if ((((v shr index) and 1) != 0) == !value) {
            val mask = 1 shl index
            currentTrackMap = if (value) (v or mask).toByte() else (v and mask.inv()).toByte()
        }
    }

    private fun calcTrackIndex(point: Offset): Track? {
        // 非屏幕可点击区域忽略
        if (point.y <= size.height * Track.SCREEN_CLICK_AREA_RATIO) return null
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

    override fun onPointerEvent(pointer: Pointer): Boolean {
        // 获取指针所在轨道
        val track = calcTrackIndex(pointer.position)
        if (track != null) {
            pointer.handle(
                down = { // 按下
                    setTrackMap(track.index, true)
                },
                up = { isClick, endTime -> // 抬起
                    setTrackMap(track.index, false)
                }
            )
        }
        return false
    }

    private fun Drawer.drawTrackLine(start: Offset, end: Offset, stroke: Float) {
        // 光带
        line(Colors.Steel4, start, end, style = Stroke(width = stroke, cap = StrokeCap.Round), alpha = 0.7f)
        // 高光
        line(Colors.White, start, end, style = Stroke(width = stroke * 0.8f, cap = StrokeCap.Round), alpha = 0.8f)
    }

    override fun Drawer.onDraw() {
        // 画点击区域
        for (track in tracks) {
            // 画区域线
            path(Colors.White, track.clickArea.areaPath, style = Stroke(5f))
            // 画区域阴影
            val (matrix, srcRect) = track.clickArea.perspectiveMatrix
            transform(matrix) {
                rect(track.clickArea.brush, position = srcRect.topLeft, size = srcRect.size)
            }
        }
        // 画按下轨道高光
        repeat(7) {
            if (getTrackMap(it)) path(color = Colors.Steel3.copy(alpha = 0.2f), path = tracks[it].areaPath)
        }
        // 画轨道射线
        for (track in tracks) drawTrackLine(vertices, track.left, Track.LINE_STROKE_WIDTH)
        // 最后一个轨道右侧射线
        drawTrackLine(vertices, tracks.last().right, Track.LINE_STROKE_WIDTH)
    }
}