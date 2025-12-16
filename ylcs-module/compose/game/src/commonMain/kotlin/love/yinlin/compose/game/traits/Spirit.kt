package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Fill
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.Manager
import kotlin.math.min

@Stable
abstract class Spirit(val manager: Manager): Positionable, PreTransform, AABB, Visible, Dynamic, Trigger {
    // Dynamic
    protected open fun onClientUpdate(tick: Long) { }

    final override fun onUpdate(tick: Long) {
        onClientUpdate(tick)
    }

    // Trigger
    protected open fun onClientEvent(tick: Long, event: Event): Boolean = false

    private val clientMatrix: Matrix get() {
        val matrix = Matrix()
        for (transform in preTransform) {
            when (transform) {
                is Transform.Translate -> matrix.translate(transform.x, transform.y)
                is Transform.Scale -> {
                    val pivot = transform.pivot ?: center
                    if (pivot != Offset.Zero) {
                        matrix.translate(pivot.x, pivot.y)
                        matrix.scale(transform.x, transform.y)
                        matrix.translate(-pivot.x, -pivot.y)
                    }
                    else matrix.scale(transform.x, transform.y)
                }
                is Transform.Rotate -> {
                    val pivot = transform.pivot ?: center
                    if (pivot != Offset.Zero) {
                        matrix.translate(pivot.x, pivot.y)
                        matrix.rotateZ(transform.degrees)
                        matrix.translate(-pivot.x, -pivot.y)
                    }
                    else matrix.rotateZ(transform.degrees)
                }
                is Transform.Matrix -> matrix *= transform.matrix
            }
        }
        matrix.invert()
        return matrix
    }

    final override fun onEvent(tick: Long, event: Event): Boolean = when (event) {
        is PointerEvent -> {
            val position = clientMatrix.map(event.position)
            if (position in this) {
                when (event) {
                    is PointerDownEvent -> onClientEvent(tick, event.reset(position))
                    is PointerUpEvent -> {
                        val rawPosition = clientMatrix.map(event.rawPosition)
                        onClientEvent(tick, event.reset(position, rawPosition))
                    }
                    is PointerMoveEvent -> {
                        val rawPosition = clientMatrix.map(event.rawPosition)
                        onClientEvent(tick, event.reset(position, rawPosition))
                    }
                }
            } else false
        }
    }

    // Visible
    override val zIndex: Int = 0

    protected abstract fun Drawer.onClientDraw()

    private fun clientDrawTransform(dt: DrawTransform) {
        for (transform in preTransform) {
            when (transform) {
                is Transform.Translate -> dt.translate(transform.x, transform.y)
                is Transform.Scale -> dt.scale(transform.x, transform.y, transform.pivot ?: center)
                is Transform.Rotate -> dt.rotate(transform.degrees, transform.pivot ?: center)
                is Transform.Matrix -> dt.transform(transform.matrix)
            }
        }
    }

    final override fun Drawer.onDraw() {
        transform(::clientDrawTransform) { onClientDraw() }
    }

    protected val topLeft: Offset get() = Offset.Zero
    protected val topRight: Offset get() = Offset(size.width, 0f)
    protected val bottomLeft: Offset get() = Offset(0f, size.height)
    protected val bottomRight: Offset get() = Offset(size.width, size.height)
    protected val leftCenter: Offset get() = Offset(0f, size.height / 2)
    protected val topCenter: Offset get() = Offset(size.width / 2, 0f)
    protected val bottomCenter: Offset get() = Offset(size.width / 2, size.height)
    protected val rightCenter: Offset get() = Offset(size.width, size.height / 2)
    protected val center: Offset get() = Offset(size.width / 2, size.height / 2)
    protected val radius: Float get() = min(size.width, size.height) / 2

    protected fun Drawer.circle(color: Color, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = circle(color, Offset.Zero, radius, alpha, style, blendMode)
    protected fun Drawer.circle(brush: Brush, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = circle(brush, Offset.Zero, radius, alpha, style, blendMode)
    protected fun Drawer.rect(color: Color, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = rect(color, Offset.Zero, size, alpha, style, blendMode)
    protected fun Drawer.rect(brush: Brush, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = rect(brush, Offset.Zero, size, alpha, style, blendMode)
    protected fun Drawer.roundRect(color: Color, radius: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = roundRect(color, radius, Offset.Zero, size, alpha, style, blendMode)
    protected fun Drawer.roundRect(brush: Brush, radius: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = roundRect(brush, radius, Offset.Zero, size, alpha, style, blendMode)
    protected fun Drawer.arc(color: Color, startAngle: Float, sweepAngle: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = arc(color, startAngle, sweepAngle, Offset.Zero, size, alpha, style, blendMode)
    protected fun Drawer.arc(brush: Brush, startAngle: Float, sweepAngle: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = arc(brush, startAngle, sweepAngle, Offset.Zero, size, alpha, style, blendMode)
    protected fun Drawer.image(image: ImageBitmap, alpha: Float = 1f, colorFilter: ColorFilter? = null, blendMode: BlendMode = BlendMode.SrcOver) = image(image, Offset.Zero, size, alpha, colorFilter, blendMode)
    protected fun Drawer.circleImage(image: ImageBitmap, alpha: Float = 1f, colorFilter: ColorFilter? = null, blendMode: BlendMode = BlendMode.SrcOver) = circleImage(image, Offset.Zero, size, alpha, colorFilter, blendMode)
    protected inline fun Drawer.clip(block: Drawer.() -> Unit) = clip(Offset.Zero, size, block)
    protected inline fun Drawer.rotate(degrees: Float, block: Drawer.() -> Unit) = rotate(degrees, center, block)
}