package love.yinlin.compose.ui.icon

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.unit.dp
import love.yinlin.compose.Colors
import love.yinlin.extension.lazyName

@Stable
class IconBuilder @PublishedApi internal constructor(private val builder: ImageVector.Builder) {
    private val nodes = ArrayList<PathNode>(32)

    fun next() {
        if (nodes.isNotEmpty()) {
            builder.addPath(
                pathData = nodes.toList(),
                pathFillType = PathFillType.NonZero,
                fill = SolidColor(Colors.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f
            )
            nodes.clear()
        }
    }

    fun path(
        brush: Brush,
        fillAlpha: Float = 1f,
        stroke: Brush? = null,
        strokeAlpha: Float = 1f,
        strokeLineWidth: Float = 1f,
        strokeLineCap: StrokeCap = StrokeCap.Butt,
        strokeLineJoin: StrokeJoin = StrokeJoin.Bevel,
        strokeLineMiter: Float = 1f,
        pathFillType: PathFillType = PathFillType.NonZero,
        pathBuilder: PathBuilder.() -> Unit
    ) {
        next()

        val subBuilder = PathBuilder()
        subBuilder.pathBuilder()
        builder.addPath(
            pathData = subBuilder.nodes,
            pathFillType = pathFillType,
            fill = brush,
            fillAlpha = fillAlpha,
            stroke = stroke,
            strokeAlpha = strokeAlpha,
            strokeLineWidth = strokeLineWidth,
            strokeLineCap = strokeLineCap,
            strokeLineJoin = strokeLineJoin,
            strokeLineMiter = strokeLineMiter
        )
    }

    fun path(
        color: Color,
        fillAlpha: Float = 1f,
        stroke: Brush? = null,
        strokeAlpha: Float = 1f,
        strokeLineWidth: Float = 1f,
        strokeLineCap: StrokeCap = StrokeCap.Butt,
        strokeLineJoin: StrokeJoin = StrokeJoin.Bevel,
        strokeLineMiter: Float = 1f,
        pathFillType: PathFillType = PathFillType.NonZero,
        pathBuilder: PathBuilder.() -> Unit
    ) {
        path(
            brush = SolidColor(color),
            fillAlpha = fillAlpha,
            stroke = stroke,
            strokeAlpha = strokeAlpha,
            strokeLineWidth = strokeLineWidth,
            strokeLineCap = strokeLineCap,
            strokeLineJoin = strokeLineJoin,
            strokeLineMiter = strokeLineMiter,
            pathFillType = pathFillType,
            pathBuilder = pathBuilder
        )
    }

    fun close() { nodes += PathNode.Close }
    fun moveTo(x: Float, y: Float) { nodes += PathNode.MoveTo(x, y) }
    fun moveToRelative(dx: Float, dy: Float) { nodes += PathNode.RelativeMoveTo(dx, dy) }
    fun lineTo(x: Float, y: Float) { nodes += PathNode.LineTo(x, y) }
    fun lineToRelative(dx: Float, dy: Float) { nodes += PathNode.RelativeLineTo(dx, dy) }
    fun horizontalLineTo(x: Float) { nodes += PathNode.HorizontalTo(x) }
    fun horizontalLineToRelative(dx: Float) { nodes += PathNode.RelativeHorizontalTo(dx) }
    fun verticalLineTo(y: Float) { nodes += PathNode.VerticalTo(y) }
    fun verticalLineToRelative(dy: Float) { nodes += PathNode.RelativeVerticalTo(dy) }
    fun curveTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) { nodes += PathNode.CurveTo(x1, y1, x2, y2, x3, y3) }
    fun curveToRelative(dx1: Float, dy1: Float, dx2: Float, dy2: Float, dx3: Float, dy3: Float) { nodes += PathNode.RelativeCurveTo(dx1, dy1, dx2, dy2, dx3, dy3) }
    fun reflectiveCurveTo(x1: Float, y1: Float, x2: Float, y2: Float) { nodes += PathNode.ReflectiveCurveTo(x1, y1, x2, y2) }
    fun reflectiveCurveToRelative(dx1: Float, dy1: Float, dx2: Float, dy2: Float) { nodes += PathNode.RelativeReflectiveCurveTo(dx1, dy1, dx2, dy2) }
    fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) { nodes += PathNode.QuadTo(x1, y1, x2, y2) }
    fun quadToRelative(dx1: Float, dy1: Float, dx2: Float, dy2: Float) { nodes += PathNode.RelativeQuadTo(dx1, dy1, dx2, dy2) }
    fun reflectiveQuadTo(x1: Float, y1: Float) { nodes += PathNode.ReflectiveQuadTo(x1, y1) }
    fun reflectiveQuadToRelative(dx1: Float, dy1: Float) { nodes += PathNode.RelativeReflectiveQuadTo(dx1, dy1) }
    fun arcTo(horizontalEllipseRadius: Float, verticalEllipseRadius: Float, theta: Float, isMoreThanHalf: Boolean, isPositiveArc: Boolean, x1: Float, y1: Float) { nodes += PathNode.ArcTo(horizontalEllipseRadius, verticalEllipseRadius, theta, isMoreThanHalf, isPositiveArc, x1, y1) }
    fun arcToRelative(a: Float, b: Float, theta: Float, isMoreThanHalf: Boolean, isPositiveArc: Boolean, dx1: Float, dy1: Float) { nodes += PathNode.RelativeArcTo(a, b, theta, isMoreThanHalf, isPositiveArc, dx1, dy1) }
}

inline fun icon(
    rawSize: Float = 24f,
    autoMirror: Boolean = false,
    crossinline block: IconBuilder.() -> Unit
) = lazyName { name ->
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = rawSize,
        viewportHeight = rawSize,
        autoMirror = autoMirror,
    )
    val iconBuilder = IconBuilder(builder)
    iconBuilder.block()
    iconBuilder.next()
    builder.build()
}