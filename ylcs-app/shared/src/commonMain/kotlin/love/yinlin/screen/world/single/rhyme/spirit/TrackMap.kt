package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.fastMapIndexed
import love.yinlin.compose.Colors
import love.yinlin.compose.Path
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Transform
import love.yinlin.compose.onCenter
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
        val Scales = intArrayOf(4, 5, 6, 1, 2, 3, 7)
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
        val BackgroundColor = Colors(0xFF1D1A24)
        // 轨道激活色
        val ActiveColor = Colors.Cyan3.copy(alpha = 0.2f)
    }

    val bottomCenter = left.onCenter(right)

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
    val perspectiveMatrix = Drawer.calcFixedPerspectiveMatrix(DynamicAction.PERSPECTIVE_K.toFloat(), left, right, slopeLeft, slopeRight)
}

@Stable
class TrackMap(
    rhymeManager: RhymeManager,
) : Spirit(rhymeManager), BoxBody {
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
    val tracksArea = arrayOf(
        tracks.first().left,
        vertices,
        tracks.last().right
    )
    val tracksAreaPath = Path(tracksArea)

    // 轨道线画刷 (注意所有轨道左右侧线高度相同)
    val trackLineBrush = Brush.verticalGradient(
        colors = listOf(Colors.White, Colors.Transparent),
        startY = Track.VIRTUAL_HEIGHT,
        endY = 0f
    )
    val trackLineSideBrush = Brush.verticalGradient(
        colors = listOf(Colors.Purple3, Colors.Transparent),
        startY = Track.VIRTUAL_HEIGHT,
        endY = 0f
    )

    // 当前按下轨道
    val activeTracks = List<Long?>(Track.Num) { null }.toMutableStateList()

    // 判定区域
    val hitLine = vertices.onLine(tracksArea[0], DynamicAction.HIT_RATIO) to vertices.onLine(tracksArea[2], DynamicAction.HIT_RATIO)
    val hitAreaData = ActionResult.entries.fastMapIndexed { index, result ->
        Path(arrayOf(
            vertices.onLine(tracksArea[0], result.startRange(DynamicAction.HIT_RATIO)),
            vertices.onLine(tracksArea[0], result.endRange(DynamicAction.HIT_RATIO)),
            vertices.onLine(tracksArea[2], result.endRange(DynamicAction.HIT_RATIO)),
            vertices.onLine(tracksArea[2], result.startRange(DynamicAction.HIT_RATIO)),
        )) to index * 0.05f
    }

    // 点击区域边界
    val clickAreaBound = size.height * (1 - ActionResult.MISS.range)

    fun calcTrackIndex(point: Offset): Pair<Track?, Boolean> {
        // 不需要计算点是否位于每个轨道三角形内，只需要计算斜率即可
        val slope = vertices.slope(point)
        val inTracks = slope <= tracks.first().slopeLeft || slope >= tracks.last().slopeRight
        // 非屏幕可点击区域忽略
        if (point.y <= clickAreaBound) return null to false
        if (slope >= 0f) { // 右侧
            for (i in 3 .. 6) {
                val (left, right)  = tracks[i].slopeRightRange
                if (slope >= right) {
                    // 点击到轨道线上的不计入
                    return (if (slope >= left) tracks[i] else null) to inTracks
                }
            }
        }
        else { // 左侧
            for (i in 3 downTo 0) {
                val (left, right)  = tracks[i].slopeLeftRange
                if (slope <= left) {
                    return (if (slope <= right) tracks[i] else null) to inTracks
                }
            }
        }
        return null to inTracks
    }

    override fun Drawer.onClientDraw() {
        // 画轨道背景
        path(Track.BackgroundColor, tracksAreaPath, alpha = 0.95f)

        // 画判定区域
        for ((area, alpha) in hitAreaData) path(Colors.Purple4, area, alpha = alpha)
        line(Colors.Purple4, hitLine.first, hitLine.second, style = Stroke(10f))

        // 其他轨道线
        for (index in 0 ..< Track.Num) {
            val track = tracks[index]
            // 画激活轨道
            if (activeTracks[index] != null) path(Track.ActiveColor, track.areaPath)

            if (index == 0) {
                path(Colors.Steel2, track.leftLineShadowAreaPath, alpha = 0.2f)
                path(trackLineSideBrush, track.leftLineAreaPath)
            }
            else {
                // 画轨道左侧射线
                path(Colors.Steel2, track.leftLineShadowAreaPath, alpha = 0.2f) // 阴影
                path(trackLineBrush, track.leftLineAreaPath) // 轨道线用三角形模拟, 这样能做出一个越远越细的效果
                path(Colors.Steel4, track.leftLineAreaPath, style = Stroke(2f), alpha = 0.75f)
            }

            if (index == tracks.lastIndex) {
                path(Colors.Steel2, track.rightLineShadowAreaPath, alpha = 0.2f)
                path(trackLineSideBrush, track.rightLineAreaPath)
            }
        }
    }
}