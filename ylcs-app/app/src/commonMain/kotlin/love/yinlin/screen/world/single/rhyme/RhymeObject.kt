package love.yinlin.screen.world.single.rhyme

import androidx.collection.lruCache
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import love.yinlin.compose.roundToIntOffset
import kotlin.math.*

// 指针数据
@Stable
internal data class Pointer(
    val id: Long,
    val position: Offset, // 按下位置
    val startTime: Long, // 按下时间
    val endTime: Long? = null, // 抬起时间
) {
    companion object {
        const val LONG_PRESS_TIMEOUT = 500L
    }

    val isDown: Boolean get() = endTime == null
    val isUp: Boolean get() = endTime != null
    val isClick: Boolean get() = endTime?.let { it - startTime < LONG_PRESS_TIMEOUT } ?: false // 是否单击
    val isLongClick: Boolean get() = endTime?.let { it - startTime >= LONG_PRESS_TIMEOUT } ?: false // 是否长按
    inline fun handle(down: () -> Unit, up: (isClick: Boolean, endTime: Long) -> Unit) = endTime?.let { up(it - startTime < LONG_PRESS_TIMEOUT, it) } ?: down()
}

internal fun Path(positions: Array<Offset>): Path = Path().apply {
    positions.firstOrNull()?.let { first ->
        moveTo(first.x, first.y)
        for (i in 1 ..< positions.size) {
            val position = positions[i]
            lineTo(position.x, position.y)
        }
        close()
    }
}

// 文本绘制缓存
@Stable
internal class TextCache(maxSize: Int = 8) {
    @Stable
    private data class CacheKey(
        val text: String,
        val height: Float,
        val fontWeight: FontWeight
    )

    private val lruCache = lruCache<CacheKey, Paragraph>(maxSize)

    fun measureText(manager: RhymeTextManager, text: String, height: Float, fontWeight: FontWeight = FontWeight.Light): Paragraph {
        val cacheKey = CacheKey(text, height, fontWeight)
        val cacheResult = lruCache[cacheKey]
        if (cacheResult != null) return cacheResult
        val newResult = manager.makeParagraph(text, height, fontWeight)
        lruCache.put(cacheKey, newResult)
        return newResult
    }
}

// 文本绘制管理器
@Stable
internal class RhymeTextManager(
    private val font: FontFamily,
    private val fontFamilyResolver: FontFamily.Resolver
) {
    private val density = Density(1f)

    fun makeParagraph(text: String, height: Float, fontWeight: FontWeight = FontWeight.Light): Paragraph {
        // 查询缓存
        val intrinsics = ParagraphIntrinsics(
            text = text,
            style = TextStyle(
                fontSize = TextUnit(height / 1.17f, TextUnitType.Sp),
                fontWeight = fontWeight,
                fontFamily = font
            ),
            annotations = emptyList(),
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            placeholders = emptyList()
        )
        return Paragraph(
            paragraphIntrinsics = intrinsics,
            constraints = Constraints.fitPrioritizingWidth(minWidth = 0, maxWidth = intrinsics.maxIntrinsicWidth.toInt(), minHeight = 0, maxHeight = height.toInt()),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }

    fun DrawScope.text(
        content: Paragraph,
        position: Offset,
        color: Color,
        shadow: Shadow? = null,
        decoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) {
        withTransform({
            translate(left = position.x, top = position.y)
            clipRect(left = 0f, top = 0f, right = content.width, bottom = content.height)
        }) {
            content.paint(
                canvas = drawContext.canvas,
                color = color,
                shadow = shadow,
                textDecoration = decoration,
                drawStyle = drawStyle,
                blendMode = blendMode
            )
        }
    }

    fun DrawScope.text(
        content: Paragraph,
        position: Offset,
        brush: Brush,
        shadow: Shadow? = null,
        decoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) {
        withTransform({
            translate(left = position.x, top = position.y)
            clipRect(left = 0f, top = 0f, right = content.width, bottom = content.height)
        }) {
            content.paint(
                canvas = drawContext.canvas,
                brush = brush,
                shadow = shadow,
                textDecoration = decoration,
                drawStyle = drawStyle,
                blendMode = blendMode
            )
        }
    }
}

// 容器
@Stable
internal sealed interface RhymeContainer {
    fun contains(position: Offset, size: Size, point: Offset): Boolean

    @Stable
    interface Rectangle : RhymeContainer {
        override fun contains(position: Offset, size: Size, point: Offset): Boolean {
            val x = point.x
            val y = point.y
            val left = position.x
            val top = position.y
            return (x >= left) and (x < left + size.width) and (y >= top) and (y < top + size.height)
        }
    }

    @Stable
    interface Circle : RhymeContainer {
        override fun contains(position: Offset, size: Size, point: Offset): Boolean {
            val a = size.width / 2
            val b = size.height / 2
            val dx = point.x - position.x - a
            val dy = point.y - position.y - b
            return (dx * dx) / (a * a) + (dy * dy) / (b * b) <= 1f
        }
    }
}

// 事件触发器
@Stable
internal fun interface RhymeEvent {
    fun onEvent(pointer: Pointer): Boolean
}

// 渲染实体
@Stable
internal sealed class RhymeObject : RhymeContainer {
    abstract val position: Offset
    abstract val size: Size
    open val transform: (DrawTransform.() -> Unit)? = null

    protected abstract fun DrawScope.onDraw(textManager: RhymeTextManager)

    operator fun contains(point: Offset): Boolean = contains(position, size, point)

    fun DrawScope.draw(textManager: RhymeTextManager) {
        withTransform({
            translate(left = position.x, top = position.y)
            transform?.invoke(this)
        }) {
            onDraw(textManager)
        }
    }

    fun DrawScope.line(color: Color, start: Offset, end: Offset, style: Stroke, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawLine(color = color, start = start, end = end, strokeWidth = style.width, cap = style.cap, pathEffect = style.pathEffect, alpha = alpha, blendMode = blendMode)

    fun DrawScope.line(brush: Brush, start: Offset, end: Offset, style: Stroke, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawLine(brush = brush, start = start, end = end, strokeWidth = style.width, cap = style.cap, pathEffect = style.pathEffect, alpha = alpha, blendMode = blendMode)

    fun DrawScope.circle(color: Color, position: Offset = center, radius: Float = max(size.width, size.height) / 2, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawCircle(color = color, radius = radius, center = position, alpha = alpha, style = style, blendMode = blendMode)

    fun DrawScope.circle(brush: Brush, position: Offset = center, radius: Float = max(size.width, size.height) / 2, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawCircle(brush = brush, radius = radius, center = position, alpha = alpha, style = style, blendMode = blendMode)

    fun DrawScope.rect(color: Color, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawRect(color = color, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)

    fun DrawScope.rect(brush: Brush, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawRect(brush = brush, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)

    fun DrawScope.path(color: Color, path: Path, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawPath(path = path, color = color, alpha = alpha, style = style, blendMode = blendMode)

    fun DrawScope.path(brush: Brush, path: Path, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawPath(path = path, brush = brush, alpha = alpha, style = style, blendMode = blendMode)

    fun DrawScope.quadrilateral(color: Color, area: Array<Offset>, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawPath(path = Path(area), color = color, alpha = alpha, style = style, blendMode = blendMode)

    fun DrawScope.quadrilateral(brush: Brush, area: Array<Offset>, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawPath(path = Path(area), brush = brush, alpha = alpha, style = style, blendMode = blendMode)

    fun DrawScope.roundRect(color: Color, radius: Float, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawRoundRect(color = color, topLeft = position, size = size, cornerRadius = CornerRadius(radius, radius), alpha = alpha, blendMode = blendMode)

    fun DrawScope.roundRect(brush: Brush, radius: Float, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawRoundRect(brush = brush, topLeft = position, size = size, cornerRadius = CornerRadius(radius, radius), alpha = alpha, blendMode = blendMode)

    fun DrawScope.arc(color: Color, startAngle: Float, sweepAngle: Float, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)

    fun DrawScope.arc(brush: Brush, startAngle: Float, sweepAngle: Float, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawArc(brush = brush, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = position, size = size, alpha = alpha, style = style, blendMode = blendMode)

    fun DrawScope.image(image: ImageBitmap, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) =
        this.drawImage(
            image = image,
            dstOffset = position.roundToIntOffset(),
            dstSize = size.roundToIntSize(),
            alpha = alpha,
            filterQuality = FilterQuality.High,
            blendMode = blendMode
        )

    fun DrawScope.circleImage(image: ImageBitmap, position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) =
        this.clipPath(Path().apply { addOval(Rect(position, size)) }) {
            this.image(
                image = image,
                position = position,
                size = size,
                alpha = alpha,
                blendMode = blendMode
            )
        }

    inline fun DrawScope.clip(position: Offset = Offset.Zero, size: Size = this@RhymeObject.size, block: DrawScope.() -> Unit) =
        this.clipRect(left = position.x, top = position.y, right = (position.x + size.width), bottom = (position.y + size.height), block = block)

    inline fun DrawScope.clip(path: Path, block: DrawScope.() -> Unit) =
        this.clipPath(path, block = block)
}

// 动态实体
@Stable
internal abstract class RhymeDynamic : RhymeObject() {
    abstract fun onUpdate(position: Long)
}