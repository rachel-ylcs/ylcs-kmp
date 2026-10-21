package love.yinlin.compose.ui.image

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

/** 缩放围绕鼠标/手指位置；translation 以容器物理像素表示。 */
@Stable
class ImageZoomState internal constructor(val maxScale: Float = 8f) {
    init { require(maxScale.isFinite() && maxScale >= 1f) }

    var scale by mutableFloatStateOf(1f)
        private set
    var translation by mutableStateOf(Offset.Zero)
        private set
    internal var viewport = IntSize.Zero
        private set
    private var content = Rect.Zero

    fun reset() {
        scale = 1f
        translation = Offset.Zero
    }

    internal fun updateBounds(viewport: IntSize, content: Rect) {
        this.viewport = viewport
        this.content = content
        translation = clamp(translation, scale)
    }

    /** 返回本次是否实际改变变换，用于把无法消费的边缘拖动交还父级 Pager。 */
    fun transform(pivot: Offset, pan: Offset = Offset.Zero, zoom: Float = 1f): Boolean {
        if (!zoom.isFinite() || zoom <= 0f || !pivot.x.isFinite() || !pivot.y.isFinite() ||
            !pan.x.isFinite() || !pan.y.isFinite() || viewport.width <= 0 || viewport.height <= 0 || content.isEmpty) return false
        val next = (scale * zoom).coerceIn(1f, maxScale)
        val ratio = next / scale
        val center = Offset(viewport.width / 2f, viewport.height / 2f)
        val offset = (pivot - center) * (1f - ratio) + translation * ratio + pan
        val clamped = clamp(offset, next)
        val changed = next != scale || clamped != translation
        scale = next
        translation = clamped
        return changed
    }

    fun toggle(pivot: Offset) {
        if (scale > 1f) reset()
        else transform(pivot, zoom = minOf(3f, maxScale))
    }

    private fun clamp(offset: Offset, scale: Float): Offset {
        val cx = viewport.width / 2f
        val cy = viewport.height / 2f
        val left = cx + (content.left - cx) * scale
        val right = cx + (content.right - cx) * scale
        val top = cy + (content.top - cy) * scale
        val bottom = cy + (content.bottom - cy) * scale
        val x = if (right - left > viewport.width) offset.x.coerceIn(viewport.width - right, -left)
        else smallContentTranslation(content.left, content.width, viewport.width.toFloat(), left, right - left)
        val y = if (bottom - top > viewport.height) offset.y.coerceIn(viewport.height - bottom, -top)
        else smallContentTranslation(content.top, content.height, viewport.height.toFloat(), top, bottom - top)
        return Offset(x, y)
    }
}

private fun smallContentTranslation(start: Float, length: Float, viewport: Float, scaledStart: Float, scaledLength: Float): Float {
    val fraction = if (viewport > length) (start / (viewport - length)).coerceIn(0f, 1f) else 0.5f
    return (viewport - scaledLength) * fraction - scaledStart
}
