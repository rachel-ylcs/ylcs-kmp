package love.yinlin.compose.game

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import love.yinlin.compose.game.traits.AABB
import love.yinlin.compose.game.traits.PreTransform
import love.yinlin.compose.game.traits.Transform

@Stable
abstract class Spirit(val manager: Manager): AABB, PreTransform {
    protected abstract fun Drawer.onDraw()

    fun Drawer.draw() {
        transform({
            for (transform in preTransform) {
                when (transform) {
                    is Transform.Translate -> translate(transform.x, transform.y)
                    is Transform.Scale -> scale(transform.x, transform.y, transform.pivot ?: center)
                    is Transform.Rotate -> rotate(transform.degrees, transform.pivot ?: center)
                }
            }
        }) {
            onDraw()
        }
    }

    fun Drawer.circle(color: Color, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = circle(color, Offset.Zero, radius, alpha, style, blendMode)
    fun Drawer.circle(brush: Brush, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = circle(brush, Offset.Zero, radius, alpha, style, blendMode)
    fun Drawer.rect(color: Color, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = rect(color, Offset.Zero, size, alpha, style, blendMode)
    fun Drawer.rect(brush: Brush, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = rect(brush, Offset.Zero, size, alpha, style, blendMode)
    fun Drawer.roundRect(color: Color, radius: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = roundRect(color, radius, Offset.Zero, size, alpha, style, blendMode)
    fun Drawer.roundRect(brush: Brush, radius: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = roundRect(brush, radius, Offset.Zero, size, alpha, style, blendMode)
    fun Drawer.arc(color: Color, startAngle: Float, sweepAngle: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = arc(color, startAngle, sweepAngle, Offset.Zero, size, alpha, style, blendMode)
    fun Drawer.arc(brush: Brush, startAngle: Float, sweepAngle: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = arc(brush, startAngle, sweepAngle, Offset.Zero, size, alpha, style, blendMode)
    fun Drawer.image(image: ImageBitmap, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) = image(image, Offset.Zero, size, alpha, blendMode)
    fun Drawer.circleImage(image: ImageBitmap, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) = circleImage(image, Offset.Zero, size, alpha, blendMode)
    fun Drawer.clip(block: Drawer.() -> Unit) = clip(Offset.Zero, size, block)
    fun Drawer.rotate(degrees: Float, block: Drawer.() -> Unit) = rotate(degrees, center, block)
}