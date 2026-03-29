package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.roundToIntSize
import love.yinlin.compose.extension.roundToIntOffset
import love.yinlin.compose.extension.translate

@Stable
class Drawer internal constructor() {
    @PublishedApi internal var scope: DrawScope? = null

    fun line(color: Color, start: Offset, end: Offset, style: Stroke, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawLine(color = color, start = start, end = end, strokeWidth = style.width, cap = style.cap, pathEffect = style.pathEffect, alpha = alpha, blendMode = blendMode)
    }

    fun line(brush: Brush, start: Offset, end: Offset, style: Stroke, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawLine(brush = brush, start = start, end = end, strokeWidth = style.width, cap = style.cap, pathEffect = style.pathEffect, alpha = alpha, blendMode = blendMode)
    }

    fun circle(color: Color, position: Offset, radius: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawCircle(color = color, radius = radius, center = position, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun circle(brush: Brush, position: Offset, radius: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawCircle(brush = brush, radius = radius, center = position, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun oval(color: Color, position: Offset, radiusX: Float, radiusY: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawOval(color = color, topLeft = position.translate(x = -radiusX, y = -radiusY), size = Size(radiusX * 2, radiusY * 2), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun oval(brush: Brush, position: Offset, radiusX: Float, radiusY: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawOval(brush = brush, topLeft = position.translate(x = -radiusX, y = -radiusY), size = Size(radiusX * 2, radiusY * 2), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun rect(color: Color, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawRect(color = color, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun rect(brush: Brush, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawRect(brush = brush, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun rect(color: Color, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawRect(color = color, topLeft = rect.topLeft, size = rect.size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun rect(brush: Brush, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawRect(brush = brush, topLeft = rect.topLeft, size = rect.size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun roundRect(color: Color, radius: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawRoundRect(color = color, topLeft = position, size = size, cornerRadius = CornerRadius(radius, radius), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun roundRect(brush: Brush, radius: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawRoundRect(brush = brush, topLeft = position, size = size, cornerRadius = CornerRadius(radius, radius), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun roundRect(color: Color, radius: Float, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawRoundRect(color = color, topLeft = rect.topLeft, size = rect.size, cornerRadius = CornerRadius(radius, radius), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun roundRect(brush: Brush, radius: Float, rect: Rect, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawRoundRect(brush = brush, topLeft = rect.topLeft, size = rect.size, cornerRadius = CornerRadius(radius, radius), alpha = alpha, style = style, blendMode = blendMode)
    }

    fun arc(color: Color, startAngle: Float, sweepAngle: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun arc(brush: Brush, startAngle: Float, sweepAngle: Float, position: Offset, size: Size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawArc(brush = brush, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun quadrilateral(color: Color, area: Array<Offset>, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawPath(path = love.yinlin.compose.extension.Path(area), color = color, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun quadrilateral(brush: Brush, area: Array<Offset>, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawPath(path = love.yinlin.compose.extension.Path(area), brush = brush, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun path(color: Color, path: Path, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawPath(path = path, color = color, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun path(brush: Brush, path: Path, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawPath(path = path, brush = brush, alpha = alpha, style = style, blendMode = blendMode)
    }

    fun image(image: ImageBitmap, position: Offset, size: Size, alpha: Float = 1f, colorFilter: ColorFilter? = null, blendMode: BlendMode = BlendMode.SrcOver) {
        scope?.drawImage(
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
        scope?.drawImage(
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
        scope?.drawImage(
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
}