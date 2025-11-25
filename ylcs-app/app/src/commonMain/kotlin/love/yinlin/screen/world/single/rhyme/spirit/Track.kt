package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager

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
        // 提示区域起始
        const val TIP_START_RATIO = 0.8f
        // 提示区域结束
        const val TIP_END_RATIO = 0.9f
        // 提示区域区间
        const val TIP_RANGE = TIP_END_RATIO - TIP_START_RATIO
    }

    val slopeLeft: Float = vertices.slope(left) // 左侧点斜率
    val slopeRight: Float = vertices.slope(right) // 右侧点斜率
    val area: Array<Offset> = arrayOf(vertices, left, right) // 轨道区域
    val areaPath: Path = Path(area) // 轨道区域路径
    // 点击区域
    val clickArea: Array<Offset> = arrayOf(
        vertices.onLine(left, (TIP_START_RATIO + VERTICES_TOP_RATIO) / (1 + VERTICES_TOP_RATIO)),
        vertices.onLine(left, (TIP_END_RATIO + VERTICES_TOP_RATIO) / (1 + VERTICES_TOP_RATIO)),
        vertices.onLine(right, (TIP_END_RATIO + VERTICES_TOP_RATIO) / (1 + VERTICES_TOP_RATIO)),
        vertices.onLine(right, (TIP_START_RATIO + VERTICES_TOP_RATIO) / (1 + VERTICES_TOP_RATIO))
    )
    val clickAreaPath: Path = Path(clickArea) // 点击区域路径
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
        // 不需要计算点是否位于每个轨道三角形内，只需要计算斜率即可
        val slope = vertices.slope(point)
        if (slope > 0f) { // 右侧
            for (i in 3 .. 6) {
                if (slope >= tracks[i].slopeRight) return tracks[i]
            }
        }
        else { // 左侧
            for (i in 3 downTo 0) {
                if (slope <= tracks[i].slopeLeft) return tracks[i]
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
            path(Colors.White, track.clickAreaPath, style = Stroke(5f))
        }
        // 画按下轨道高光
        repeat(7) {
            if (getTrackMap(it)) path(color = Colors.Steel3.copy(alpha = 0.2f), path = tracks[it].areaPath)
        }
        // 画轨道射线
        for (track in tracks) drawTrackLine(vertices, track.left, 20f)
        // 最后一个轨道右侧射线
        drawTrackLine(vertices, tracks.last().right, 20f)
    }
}