package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import love.yinlin.compose.extension.roundToIntOffset
import love.yinlin.compose.extension.translate

@Stable
class Drawer internal constructor(
    fontFamilyResolver: FontFamily.Resolver,
    fontProvider: FontProvider
) : PrepareDrawer(fontFamilyResolver, fontProvider) {
    @PublishedApi internal var rawScope: DrawScope? = null

    internal inline fun withRawScope(scope: DrawScope, block: Drawer.() -> Unit) {
        rawScope = scope
        block()
        rawScope = null
    }

    // 拓展函数 - Draw

    fun line(color: Color, start: Offset, end: Offset, style: Stroke, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawLine(color = color, start = start, end = end, strokeWidth = style.width, cap = style.cap, pathEffect = style.pathEffect, alpha = alpha, blendMode = blendMode)
    }

    fun line(brush: Brush, start: Offset, end: Offset, style: Stroke, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawLine(brush = brush, start = start, end = end, strokeWidth = style.width, cap = style.cap, pathEffect = style.pathEffect, alpha = alpha, blendMode = blendMode)
    }

    fun circle(color: Color, position: Offset, radius: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawCircle(color = color, radius = radius, center = position, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun circle(brush: Brush, position: Offset, radius: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawCircle(brush = brush, radius = radius, center = position, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun oval(color: Color, position: Offset, radiusX: Float, radiusY: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawOval(color = color, topLeft = position.translate(x = -radiusX, y = -radiusY), size = Size(radiusX * 2, radiusY * 2), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun oval(brush: Brush, position: Offset, radiusX: Float, radiusY: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawOval(brush = brush, topLeft = position.translate(x = -radiusX, y = -radiusY), size = Size(radiusX * 2, radiusY * 2), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun rect(color: Color, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawRect(color = color, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun rect(brush: Brush, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawRect(brush = brush, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun rect(color: Color, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawRect(color = color, topLeft = rect.topLeft, size = rect.size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun rect(brush: Brush, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawRect(brush = brush, topLeft = rect.topLeft, size = rect.size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun roundRect(color: Color, radius: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawRoundRect(color = color, topLeft = position, size = size, cornerRadius = CornerRadius(radius, radius), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun roundRect(brush: Brush, radius: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawRoundRect(brush = brush, topLeft = position, size = size, cornerRadius = CornerRadius(radius, radius), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun roundRect(color: Color, radius: Float, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawRoundRect(color = color, topLeft = rect.topLeft, size = rect.size, cornerRadius = CornerRadius(radius, radius), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun roundRect(brush: Brush, radius: Float, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawRoundRect(brush = brush, topLeft = rect.topLeft, size = rect.size, cornerRadius = CornerRadius(radius, radius), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun arc(color: Color, startAngle: Float, sweepAngle: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun arc(brush: Brush, startAngle: Float, sweepAngle: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawArc(brush = brush, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun quadrilateral(color: Color, area: Array<Offset>, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawPath(path = love.yinlin.compose.extension.Path(area), color = color, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun quadrilateral(brush: Brush, area: Array<Offset>, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawPath(path = love.yinlin.compose.extension.Path(area), brush = brush, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun path(color: Color, path: Path, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawPath(path = path, color = color, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun path(brush: Brush, path: Path, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawPath(path = path, brush = brush, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun image(image: ImageBitmap, position: Offset, size: Size, alpha: Float = 1f, colorFilter: ColorFilter? = null, blendMode: BlendMode = BlendMode.SrcOver) {
        rawScope?.drawImage(
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
        rawScope?.drawImage(
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
        rawScope?.drawImage(
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

    // 文本绘制

    internal inline fun text(
        content: TextGraph?,
        textPosition: Offset,
        textSize: Size,
        textAlign: TextAlign,
        block: (Canvas, Paragraph) -> Unit,
    ) {
        rawScope?.apply {
            content?.let { textGraph ->
                withTransform({
                    // 计算文本实际宽度和缩放
                    val widthScale = textGraph.widthScale(textSize.height)
                    val textWidth = unpackFloat1(widthScale)
                    val scale = unpackFloat2(widthScale)
                    val actualWidth = textSize.width
                    // 计算对齐方式偏移
                    val offsetX = when (textAlign) {
                        TextAlign.Start, TextAlign.Unspecified, TextAlign.Left -> 0f
                        TextAlign.Center -> (actualWidth - textWidth) / 2
                        TextAlign.End, TextAlign.Right -> actualWidth - textWidth
                        else -> 0f
                    }
                    // 偏移
                    translate(textPosition.x + offsetX, textPosition.y)
                    // 光栅缩放
                    scale(scale, scale, Offset.Zero)
                }) {
                    block(drawContext.canvas, textGraph.paragraph)
                }
            }
        }
    }

    fun text(
        content: TextGraph?,
        position: Offset,
        size: Size,
        color: Color,
        textAlign: TextAlign = TextAlign.Start,
        shadow: Shadow? = null,
        decoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) {
        text(content, position, size, textAlign) { canvas, paragraph ->
            paragraph.paint(
                canvas = canvas,
                color = color,
                shadow = shadow,
                textDecoration = decoration,
                drawStyle = drawStyle,
                blendMode = blendMode
            )
        }
    }

    fun text(
        content: TextGraph?,
        position: Offset,
        size: Size,
        brush: Brush,
        textAlign: TextAlign = TextAlign.Start,
        shadow: Shadow? = null,
        decoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) {
        text(content, position, size, textAlign) { canvas, paragraph ->
            paragraph.paint(
                canvas = canvas,
                brush = brush,
                shadow = shadow,
                textDecoration = decoration,
                drawStyle = drawStyle,
                blendMode = blendMode
            )
        }
    }

    // 拓展函数 - Transform

    inline fun translate(x: Float, y: Float, block: Drawer.() -> Unit) {
        rawScope?.translate(x, y) { block()}
    }

    inline fun translate(offset: Offset, block: Drawer.() -> Unit) {
        rawScope?.translate(offset.x, offset.y) { block() }
    }

    inline fun scale(ratio: Float, pivot: Offset, block: Drawer.() -> Unit) {
        rawScope?.scale(ratio, ratio, pivot) { block() }
    }

    inline fun scale(x: Float, y: Float, pivot: Offset, block: Drawer.() -> Unit) {
        rawScope?.scale(x, y, pivot) { block() }
    }

    inline fun rotate(degrees: Float, pivot: Offset, block: Drawer.() -> Unit) {
        rawScope?.rotate(degrees, pivot) { block() }
    }

    inline fun clip(position: Offset, size: Size, block: Drawer.() -> Unit) {
        rawScope?.clipRect(left = position.x, top = position.y, right = (position.x + size.width), bottom = (position.y + size.height)) { block() }
    }

    inline fun clip(rect: Rect, block: Drawer.() -> Unit) {
        clip(rect.topLeft, rect.size, block)
    }

    inline fun clip(path: Path, block: Drawer.() -> Unit) {
        rawScope?.clipPath(path) { block() }
    }

    inline fun transform(matrix: Matrix, block: Drawer.() -> Unit) {
        rawScope?.withTransform({ transform(matrix) }) { block() }
    }

    inline fun transform(transformBlock: DrawTransform.() -> Unit, block: Drawer.() -> Unit) {
        rawScope?.withTransform(transformBlock) { block() }
    }
}