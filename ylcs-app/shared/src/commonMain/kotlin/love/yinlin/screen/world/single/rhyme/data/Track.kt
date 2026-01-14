package love.yinlin.screen.world.single.rhyme.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import love.yinlin.compose.Colors
import love.yinlin.compose.extension.*
import love.yinlin.compose.game.Drawer

// 轨道
@Stable
class Track(
    val index: Int, // 索引
    val scale: Int, // 音阶
    val left: Offset, // 左侧点
    val right: Offset, // 右侧点
) {
    companion object {
        // 线宽
        const val LINE_STROKE_WIDTH = 20f
        // 轨道背景色
        val BackgroundColor = Colors(0xFF1D1A24)
        // 轨道激活色
        val ActiveColor = Colors.Cyan1.copy(alpha = 0.1f)
    }

    // 关键点
    val bottomCenter = left.onCenter(right) // 底部中点
    val bottomTailLeft = left.onLine(right, 1 / 6f) // 拖尾线左侧
    val bottomTailRight = right.onLine(left, 1 / 6f) // 拖尾线右侧

    // 位置
    val isLeft = index < Tracks.Size / 2
    val isRight = index > (if (Tracks.Size % 2 == 0) Tracks.Size / 2 - 1 else Tracks.Size / 2)
    val isCenter = index == (if (Tracks.Size % 2 == 0) -1 else Tracks.Size / 2)

    // 轨道线
    val leftLineLeft = left.translate(x = -LINE_STROKE_WIDTH / 2)
    val leftLineRight = left.translate(x = LINE_STROKE_WIDTH / 2)
    val leftLineArea = arrayOf(left.translate(x = -LINE_STROKE_WIDTH / 4), Tracks.Vertices, left.translate(x = LINE_STROKE_WIDTH / 4))
    val leftLineAreaPath = Path(leftLineArea)
    val leftLineShadowAreaPath = Path(arrayOf(leftLineLeft, Tracks.Vertices, leftLineRight))
    val rightLineLeft = right.translate(x = -LINE_STROKE_WIDTH / 2)
    val rightLineRight = right.translate(x = LINE_STROKE_WIDTH / 2)
    val rightLineArea = arrayOf(right.translate(x = -LINE_STROKE_WIDTH / 4), Tracks.Vertices, right.translate(x = LINE_STROKE_WIDTH / 4))
    val rightLineAreaPath = Path(rightLineArea)
    val rightLineShadowAreaPath = Path(arrayOf(rightLineLeft, Tracks.Vertices, rightLineRight))
    // 轨道线斜率范围
    val slopeLeft = Tracks.Vertices.slope(left)
    val slopeLeftRange: Pair<Float, Float> = Tracks.Vertices.slope(leftLineLeft) to Tracks.Vertices.slope(leftLineRight)
    val slopeRight = Tracks.Vertices.slope(right)
    val slopeRightRange: Pair<Float, Float> = Tracks.Vertices.slope(rightLineLeft) to Tracks.Vertices.slope(rightLineRight)
    // 轨道区域
    val area: Array<Offset> = arrayOf(Tracks.Vertices, left, right)
    val areaPath: Path = Path(area)
    // 音符透视矩阵
    val notePerspectiveMatrix = Drawer.calcFixedPerspectiveMatrix(DynamicAction.PERSPECTIVE_K.toFloat(), left, right, slopeLeft, slopeRight)

    fun plainRect(ratio: Float, sizeRatio: Float, scaleRatio: Float = 1f): Rect {
        val plainLeft = Tracks.Vertices.onLine(left, ratio)
        val plainRight = Tracks.Vertices.onLine(right, ratio)
        val plainWidth = plainRight.x - plainLeft.x
        val plainHeight = plainWidth / sizeRatio
        val actualWidth = plainWidth * scaleRatio
        val actualHeight = plainHeight * scaleRatio
        return Rect(plainLeft.translate(x = (1 - scaleRatio) * plainWidth / 2, y = -actualHeight / 2), Size(actualWidth, actualHeight))
    }
}