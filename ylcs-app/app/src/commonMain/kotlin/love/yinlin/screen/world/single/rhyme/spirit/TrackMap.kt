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
import love.yinlin.compose.blend
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.Pointer
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Event
import love.yinlin.compose.game.traits.PointerEvent
import love.yinlin.compose.game.traits.Transform
import love.yinlin.compose.onLine
import love.yinlin.compose.slope
import love.yinlin.compose.translate
import love.yinlin.screen.world.single.rhyme.RhymeConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager

// 可点击区域
@Stable
class TrackClickArea(vertices: Offset, left: Offset, right: Offset) {
    companion object {
        // 点击区域起始
        const val START_RATIO = 0.8f
        // 点击区域结束
        const val END_RATIO = 0.9f
        // 点击区域中间
        const val CENTER_RATIO = (START_RATIO + END_RATIO) / 2
        // 点击区域区间
        const val RANGE = END_RATIO - START_RATIO
    }
}

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
        val Scales = arrayOf(4, 3, 2, 1, 5, 6, 7)
        // 顶点偏移比率
        const val VERTICES_TOP_RATIO = 0.2f
        // 虚拟顶点
        const val VIRTUAL_TOP = VERTICES_TOP_RATIO * RhymeConfig.HEIGHT
        // 虚拟画布高度
        const val VIRTUAL_HEIGHT = RhymeConfig.HEIGHT + VIRTUAL_TOP
        // 数量
        val Num = Scales.size
        // 线宽
        const val LINE_STROKE_WIDTH = 20f
        // 轨道背景色
        val BackgroundColor = Colors.Black.copy(alpha = 0.4f)
        // 轨道激活色
        val ActiveColor = BackgroundColor.blend(Colors.Cyan3.copy(alpha = 0.2f))
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
    // 点击区域
    val clickArea = TrackClickArea(vertices, left, right)
}

// 激活轨道
@Stable
class ActiveTrack {
    private val pointers = List<Pointer?>(Track.Num) { null }.toMutableStateList()

    operator fun get(index: Int): Boolean = pointers[index] != null

    fun safeSet(index: Int, pointer: Pointer) {
        // 防止多指按下同一个轨道
        if (pointer.isDown) { // 按下
            if (pointers[index] == null) {
                pointers[index] = pointer
            }
        }
        else { // 抬起
            if (pointers.indexOfFirst { it?.id == pointer.id } == index) {
                pointers[index] = null
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
        const val SCREEN_CLICK_AREA_RATIO = 0.75f
    }

    override val preTransform: List<Transform> = listOf(Transform.Translate(0f, -Track.VIRTUAL_TOP))
    override val size: Size = Size(RhymeConfig.WIDTH, Track.VIRTUAL_HEIGHT)

    // 顶点
    val vertices = Offset(size.width / 2, 0f)

    // 轨道
    val tracks = buildList {
        val trackWidth = this@TrackMap.size.width / Track.Num
        val bottom = this@TrackMap.size.height
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

    // 轨道线画刷 (注意所有轨道左右侧线高度相同)
    val trackLineBrush = Brush.verticalGradient(
        colors = listOf(Colors.White, Colors.Transparent),
        startY = this@TrackMap.size.height,
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

    override fun onClientEvent(event: Event): Boolean {
        return when (event) {
            is PointerEvent -> {
                // 获取指针所在轨道
                val pointer = event.pointer
                val track = calcTrackIndex(pointer.position)
                if (track != null) active.safeSet(track.index, pointer)
                track != null
            }
        }
    }

    private fun Drawer.drawLeftTrackLine(track: Track) {
        // 阴影
        path(Colors.Steel2, track.leftLineShadowAreaPath, alpha = 0.2f)
        // 轨道线用三角形模拟, 这样能做出一个越远越细的效果
        path(trackLineBrush, track.leftLineAreaPath)
        path(Colors.Steel4, track.leftLineAreaPath, 0.75f, Stroke(2f))
    }

    private fun Drawer.drawLastTrackLine() {
        val track = tracks.last()
        path(Colors.Steel2, track.rightLineShadowAreaPath, alpha = 0.2f)
        path(trackLineBrush, track.rightLineAreaPath)
        path(Colors.Steel4, track.rightLineAreaPath, 0.75f, Stroke(2f))
    }

    override fun Drawer.onClientDraw() {
        for (index in 0 ..< Track.Num) {
            val track = tracks[index]
            // 画轨道背景
            path(
                color = if (active[index]) Track.ActiveColor else Track.BackgroundColor,
                path = track.areaPath
            )

            // 画点击区域横线
            line(Colors.Ghost, vertices.onLine(track.left, TrackClickArea.START_RATIO), vertices.onLine(track.right, TrackClickArea.START_RATIO), Stroke(5f), 0.6f)
            line(Colors.Ghost, vertices.onLine(track.left, TrackClickArea.END_RATIO), vertices.onLine(track.right, TrackClickArea.END_RATIO), Stroke(5f), 0.6f)

            // 画轨道射线
            drawLeftTrackLine(track)
        }
        // 最后一个轨道右侧射线
        drawLastTrackLine()
    }
}