package love.yinlin.compose.game

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.roundToIntSize
import love.yinlin.compose.Path
import love.yinlin.compose.graphics.AnimatedWebp
import love.yinlin.compose.roundToIntOffset
import love.yinlin.compose.translate

@Stable
class Drawer(
    val scope: DrawScope,
    private val textDrawer: TextDrawer
) {
    // 绘制形状

    fun line(color: Color, start: Offset, end: Offset, style: Stroke, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawLine(color = color, start = start, end = end, strokeWidth = style.width, cap = style.cap, pathEffect = style.pathEffect, alpha = alpha, blendMode = blendMode)
    }

    fun line(brush: Brush, start: Offset, end: Offset, style: Stroke, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawLine(brush = brush, start = start, end = end, strokeWidth = style.width, cap = style.cap, pathEffect = style.pathEffect, alpha = alpha, blendMode = blendMode)
    }

    fun circle(color: Color, position: Offset, radius: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawCircle(color = color, radius = radius, center = position, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun circle(brush: Brush, position: Offset, radius: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawCircle(brush = brush, radius = radius, center = position, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun oval(color: Color, position: Offset, radiusX: Float, radiusY: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawOval(color = color, topLeft = position.translate(x = -radiusX, y = -radiusY), size = Size(radiusX * 2, radiusY * 2), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun oval(brush: Brush, position: Offset, radiusX: Float, radiusY: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawOval(brush = brush, topLeft = position.translate(x = -radiusX, y = -radiusY), size = Size(radiusX * 2, radiusY * 2), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun rect(color: Color, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawRect(color = color, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun rect(brush: Brush, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawRect(brush = brush, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun rect(color: Color, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawRect(color = color, topLeft = rect.topLeft, size = rect.size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun rect(brush: Brush, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawRect(brush = brush, topLeft = rect.topLeft, size = rect.size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun roundRect(color: Color, radius: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawRoundRect(color = color, topLeft = position, size = size, cornerRadius = CornerRadius(radius, radius), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun roundRect(brush: Brush, radius: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawRoundRect(brush = brush, topLeft = position, size = size, cornerRadius = CornerRadius(radius, radius), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun roundRect(color: Color, radius: Float, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawRoundRect(color = color, topLeft = rect.topLeft, size = rect.size, cornerRadius = CornerRadius(radius, radius), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun roundRect(brush: Brush, radius: Float, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawRoundRect(brush = brush, topLeft = rect.topLeft, size = rect.size, cornerRadius = CornerRadius(radius, radius), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun arc(color: Color, startAngle: Float, sweepAngle: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun arc(brush: Brush, startAngle: Float, sweepAngle: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawArc(brush = brush, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun quadrilateral(color: Color, area: Array<Offset>, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawPath(path = Path(area), color = color, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun quadrilateral(brush: Brush, area: Array<Offset>, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawPath(path = Path(area), brush = brush, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun path(color: Color, path: Path, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawPath(path = path, color = color, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun path(brush: Brush, path: Path, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawPath(path = path, brush = brush, alpha = alpha, style = style, blendMode = blendMode)
    }

    // 绘制图片

    fun image(image: ImageBitmap, position: Offset, size: Size, alpha: Float = 1f, colorFilter: ColorFilter? = null, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawImage(
            image = image,
            dstOffset = position.roundToIntOffset(),
            dstSize = size.roundToIntSize(),
            alpha = alpha,
            colorFilter = colorFilter,
            blendMode = blendMode,
            filterQuality = FilterQuality.High,
        )
    }

    fun image(image: ImageBitmap, rect: Rect, alpha: Float = 1f, colorFilter: ColorFilter? = null, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawImage(
            image = image,
            dstOffset = rect.topLeft.roundToIntOffset(),
            dstSize = rect.size.roundToIntSize(),
            alpha = alpha,
            colorFilter = colorFilter,
            blendMode = blendMode,
            filterQuality = FilterQuality.High,
        )
    }

    fun image(image: ImageBitmap, src: Rect, dst: Rect, alpha: Float = 1f, colorFilter: ColorFilter? = null, blendMode: BlendMode = BlendMode.SrcOver) {
        scope.drawImage(
            image = image,
            srcOffset = src.topLeft.roundToIntOffset(),
            srcSize = src.size.roundToIntSize(),
            dstOffset = dst.topLeft.roundToIntOffset(),
            dstSize = dst.size.roundToIntSize(),
            alpha = alpha,
            colorFilter = colorFilter,
            blendMode = blendMode,
            filterQuality = FilterQuality.High,
        )
    }

    fun circleImage(image: ImageBitmap, position: Offset, size: Size, alpha: Float = 1f, colorFilter: ColorFilter? = null, blendMode: BlendMode = BlendMode.SrcOver) {
        clip(Path().apply { addOval(Rect(position, size)) }) { image(image, position, size, alpha, colorFilter, blendMode) }
    }

    fun circleImage(image: ImageBitmap, rect: Rect, alpha: Float = 1f, colorFilter: ColorFilter? = null, blendMode: BlendMode = BlendMode.SrcOver) {
        val position = rect.topLeft
        val size = rect.size
        clip(Path().apply { addOval(Rect(position, size)) }) { image(image, position, size, alpha, colorFilter, blendMode) }
    }

    // 绘制文字

    fun measureText(
        textCache: TextDrawer.Cache,
        text: String,
        textHeight: Float,
        fontWeight: FontWeight = FontWeight.Light,
        fontStyle: FontStyle = FontStyle.Normal,
        letterSpacing: Float = 0.0625f,
        fontIndex: Int = 0
    ): Paragraph = textCache.measureText(textDrawer, text, textHeight, fontWeight, fontStyle, letterSpacing, fontIndex)

    fun text(content: Paragraph, color: Color, shadow: Shadow? = null, decoration: TextDecoration? = null, drawStyle: DrawStyle? = null, blendMode: BlendMode = DrawScope.DefaultBlendMode) {
        content.paint(
            canvas = scope.drawContext.canvas,
            color = color,
            shadow = shadow,
            textDecoration = decoration,
            drawStyle = drawStyle,
            blendMode = blendMode
        )
    }

    fun text(content: Paragraph, brush: Brush, shadow: Shadow? = null, decoration: TextDecoration? = null, drawStyle: DrawStyle? = null, blendMode: BlendMode = DrawScope.DefaultBlendMode) {
        content.paint(
            canvas = scope.drawContext.canvas,
            brush = brush,
            shadow = shadow,
            textDecoration = decoration,
            drawStyle = drawStyle,
            blendMode = blendMode
        )
    }

    fun text(content: Paragraph, color: Color, position: Offset, shadow: Shadow? = null, decoration: TextDecoration? = null, drawStyle: DrawStyle? = null, blendMode: BlendMode = DrawScope.DefaultBlendMode) {
        translate(position) {
            text(content, color, shadow, decoration, drawStyle, blendMode)
        }
    }

    fun text(content: Paragraph, brush: Brush, position: Offset, shadow: Shadow? = null, decoration: TextDecoration? = null, drawStyle: DrawStyle? = null, blendMode: BlendMode = DrawScope.DefaultBlendMode) {
        translate(position) {
            text(content, brush, shadow, decoration, drawStyle, blendMode)
        }
    }

    // 绘制动图
    fun drawAnimatedWebp(image: AnimatedWebp, frame: Int, position: Offset, size: Size, alpha: Float = 1f, colorFilter: ColorFilter? = null, blendMode: BlendMode = BlendMode.SrcOver) {
        image.apply {
            scope.drawFrame(frame, position, size, alpha, colorFilter, blendMode)
        }
    }

    fun drawAnimatedWebp(image: AnimatedWebp, frame: Int, dst: Rect, alpha: Float = 1f, colorFilter: ColorFilter? = null, blendMode: BlendMode = BlendMode.SrcOver) {
        image.apply {
            scope.drawFrame(frame, dst, alpha, colorFilter, blendMode)
        }
    }

    // 转换

    inline fun translate(x: Float, y: Float, block: Drawer.() -> Unit) {
        scope.translate(x, y) { block()}
    }

    inline fun translate(offset: Offset, block: Drawer.() -> Unit) {
        scope.translate(offset.x, offset.y) { block() }
    }

    inline fun scale(ratio: Float, pivot: Offset, block: Drawer.() -> Unit) {
        scope.scale(ratio, ratio, pivot) { block() }
    }

    inline fun scale(x: Float, y: Float, pivot: Offset, block: Drawer.() -> Unit) {
        scope.scale(x, y, pivot) { block() }
    }

    inline fun rotate(degrees: Float, pivot: Offset, block: Drawer.() -> Unit) {
        scope.rotate(degrees, pivot) { block() }
    }

    inline fun clip(position: Offset, size: Size, block: Drawer.() -> Unit) {
        scope.clipRect(left = position.x, top = position.y, right = (position.x + size.width), bottom = (position.y + size.height)) { block() }
    }

    inline fun clip(rect: Rect, block: Drawer.() -> Unit) {
        clip(rect.topLeft, rect.size, block)
    }

    inline fun clip(path: Path, block: Drawer.() -> Unit) {
        scope.clipPath(path) { block() }
    }

    inline fun transform(matrix: Matrix, block: Drawer.() -> Unit) {
        scope.withTransform({ transform(matrix) }) { block() }
    }

    inline fun perspective(src: Rect, dst: Array<Offset>, block: Drawer.() -> Unit) {
        scope.withTransform({ perspective(src, dst) }) { block() }
    }

    inline fun fixedPerspective(ratio: Float = 1f, left: Offset, right: Offset, slopeLeft: Float, slopeRight: Float, block: Drawer.(Rect, Array<Offset>) -> Unit) {
        var result: Pair<Rect, Array<Offset>>? = null
        scope.withTransform({
            result = fixedPerspective(ratio, left, right, slopeLeft, slopeRight)
        }) {
            result?.let { block(it.first, it.second) }
        }
    }

    inline fun transform(transformBlock: DrawTransform.() -> Unit, block: Drawer.() -> Unit) {
        scope.withTransform(transformBlock) { block() }
    }

    fun DrawTransform.translate(offset: Offset) = translate(offset.x, offset.y)

    fun DrawTransform.scale(ratio: Float, pivot: Offset) = scale(ratio, ratio, pivot)

    fun DrawTransform.flipX(pivot: Offset = center) = scale(-1f, 1f, pivot)

    fun DrawTransform.flipY(pivot: Offset = center) = scale(1f, -1f, pivot)

    fun DrawTransform.perspective(src: Rect, dst: Array<Offset>) {
        transform(calcPerspectiveMatrix(src, dst))
    }

    // 固定中边的透视变换
    // 透视边与源矩形边重合
    // left 和 right 在同一直线上
    fun DrawTransform.fixedPerspective(ratio: Float = 1f, left: Offset, right: Offset, slopeLeft: Float, slopeRight: Float): Pair<Rect, Array<Offset>> {
        val (matrix, src, dst) = calcFixedPerspectiveMatrix(ratio, left, right, slopeLeft, slopeRight)
        transform(matrix)
        return src to dst
    }

    companion object {
        // 计算透视变换(dst为左上角逆时针)
        fun calcPerspectiveMatrix(src: Rect, dst: Array<Offset>): Matrix {
            fun mapUnitSquareToQuad(p0: Offset, p1: Offset, p2: Offset, p3: Offset): Matrix {
                val (x0, y0) = p0
                val (x1, y1) = p1
                val (x2, y2) = p2
                val (x3, y3) = p3

                val dx1 = x1 - x2
                val dx2 = x3 - x2
                val dx3 = x0 - x1 + x2 - x3
                val dy1 = y1 - y2
                val dy2 = y3 - y2
                val dy3 = y0 - y1 + y2 - y3

                val m = Matrix()
                val values = m.values

                if (dx3 == 0f && dy3 == 0f) {
                    // 仿射变换
                    values[Matrix.ScaleX] = x1 - x0
                    values[Matrix.SkewX]  = x3 - x0
                    values[Matrix.TranslateX] = x0
                    values[Matrix.SkewY]  = y1 - y0
                    values[Matrix.ScaleY] = y3 - y0
                    values[Matrix.TranslateY] = y0
                    values[Matrix.Perspective0] = 0f
                    values[Matrix.Perspective1] = 0f
                    values[Matrix.Perspective2] = 1f
                } else {
                    // 透视变换
                    val det1 = dx1 * dy2 - dx2 * dy1
                    if (det1 == 0f) return Matrix()

                    val g = (dx3 * dy2 - dx2 * dy3) / det1
                    val h = (dx1 * dy3 - dx3 * dy1) / det1

                    val a = x1 - x0 + g * x1
                    val b = x3 - x0 + h * x3
                    val c = x0
                    val d = y1 - y0 + g * y1
                    val e = y3 - y0 + h * y3
                    val f = y0

                    values[Matrix.ScaleX] = a
                    values[Matrix.SkewX]  = b
                    values[Matrix.TranslateX] = c
                    values[Matrix.SkewY]  = d
                    values[Matrix.ScaleY] = e
                    values[Matrix.TranslateY] = f
                    values[Matrix.Perspective0] = g
                    values[Matrix.Perspective1] = h
                    values[Matrix.Perspective2] = 1f
                }
                return m
            }

            val srcMatrix = mapUnitSquareToQuad(src.topLeft, src.topRight, src.bottomRight, src.bottomLeft)
            val dstMatrix = mapUnitSquareToQuad(dst[0], dst[3], dst[2], dst[1])
            srcMatrix.invert()
            srcMatrix *= dstMatrix
            return srcMatrix
        }

        fun calcFixedPerspectiveMatrix(ratio: Float = 1f, left: Offset, right: Offset, slopeLeft: Float, slopeRight: Float): Triple<Matrix, Rect, Array<Offset>> {
            val width = right.x - left.x
            val height = width / ratio
            val leftOffset = height / 2 / slopeLeft
            val rightOffset = height / 2 / slopeRight
            val src = Rect(left.translate(y = -height / 2), Size(width, height))
            val dst = arrayOf(
                left.translate(x = -leftOffset, y = -height / 2),
                left.translate(x = leftOffset, y = height / 2),
                right.translate(x = rightOffset, y = height / 2),
                right.translate(x = -rightOffset, y = -height / 2)
            )
            val matrix = calcPerspectiveMatrix(src, dst)
            return Triple(matrix, src, dst)
        }
    }
}