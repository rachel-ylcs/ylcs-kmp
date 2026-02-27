package love.yinlin.common.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.fastMapIndexed
import love.yinlin.compose.Colors
import love.yinlin.compose.extension.Path
import love.yinlin.compose.extension.onLine
import love.yinlin.compose.extension.slope
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Transform
import love.yinlin.common.rhyme.RhymeDifficulty
import love.yinlin.common.rhyme.RhymeManager
import love.yinlin.common.rhyme.RhymePlayConfig
import love.yinlin.common.rhyme.data.ActionResult
import love.yinlin.common.rhyme.data.DynamicAction
import love.yinlin.common.rhyme.data.Track
import love.yinlin.common.rhyme.data.Tracks

@Stable
class TrackMap(
    rhymeManager: RhymeManager,
    playConfig: RhymePlayConfig,
) : Spirit(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = listOf(Transform.Translate(0f, -Tracks.VirtualTopHeight))
    override val size: Size = Size(Tracks.VirutalWidth, Tracks.VirtualHeight)

    // 难度影响
    private val showHitArea = playConfig.difficulty < RhymeDifficulty.Hard

    // 轨道区域
    val tracksArea = arrayOf(Tracks.first().left, Tracks.Vertices, Tracks.last().right)
    val tracksAreaPath = Path(tracksArea)

    // 轨道线画刷 (注意所有轨道左右侧线高度相同)
    val trackLineBrush = Brush.verticalGradient(listOf(Colors.White, Colors.Transparent), Tracks.VirtualHeight, 0f)
    val trackLineSideBrush = Brush.verticalGradient(listOf(Colors.Purple3, Colors.Transparent), Tracks.VirtualHeight, 0f)

    // 当前按下轨道
    val activeTracks = List<Long?>(Tracks.Size) { null }.toMutableStateList()

    // 判定区域
    val hitLine = Tracks.Vertices.onLine(tracksArea[0], DynamicAction.HIT_RATIO) to Tracks.Vertices.onLine(tracksArea[2], DynamicAction.HIT_RATIO)
    val hitAreaData = ActionResult.entries.fastMapIndexed { index, result ->
        Path(arrayOf(
            Tracks.Vertices.onLine(tracksArea[0], result.startRange(DynamicAction.HIT_RATIO)),
            Tracks.Vertices.onLine(tracksArea[0], result.endRange(DynamicAction.HIT_RATIO)),
            Tracks.Vertices.onLine(tracksArea[2], result.endRange(DynamicAction.HIT_RATIO)),
            Tracks.Vertices.onLine(tracksArea[2], result.startRange(DynamicAction.HIT_RATIO)),
        )) to index * 0.05f
    }

    // 点击区域边界
    val clickAreaBound = Tracks.VirtualHeight * (1 - ActionResult.MISS.range)

    fun calcTrackIndex(point: Offset): Pair<Track?, Boolean> {
        // 不需要计算点是否位于每个轨道三角形内，只需要计算斜率即可
        val slope = Tracks.Vertices.slope(point)
        val inTracks = slope <= Tracks.first().slopeLeft || slope >= Tracks.last().slopeRight
        // 非屏幕可点击区域忽略
        if (point.y <= clickAreaBound) return null to false
        if (slope >= 0f) { // 右侧
            for (i in 3 .. 6) {
                val (left, right)  = Tracks[i].slopeRightRange
                if (slope >= right) {
                    // 点击到轨道线上的不计入
                    return (if (slope >= left) Tracks[i] else null) to inTracks
                }
            }
        }
        else { // 左侧
            for (i in 3 downTo 0) {
                val (left, right)  = Tracks[i].slopeLeftRange
                if (slope <= left) {
                    return (if (slope <= right) Tracks[i] else null) to inTracks
                }
            }
        }
        return null to inTracks
    }

    override fun Drawer.onClientDraw() {
        // 画轨道背景
        path(Track.BackgroundColor, tracksAreaPath, alpha = 0.95f)

        // 画判定区域
        if (showHitArea) {
            for ((area, alpha) in hitAreaData) path(Colors.Purple4, area, alpha = alpha)
        }

        line(Colors.Purple4, hitLine.first, hitLine.second, style = Stroke(10f))

        // 其他轨道线
        Tracks.foreachIndexed { index, track ->
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

            if (index == Tracks.lastIndex) {
                path(Colors.Steel2, track.rightLineShadowAreaPath, alpha = 0.2f)
                path(trackLineSideBrush, track.rightLineAreaPath)
            }
        }
    }
}