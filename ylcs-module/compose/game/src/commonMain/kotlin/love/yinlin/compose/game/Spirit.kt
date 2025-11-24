package love.yinlin.compose.game

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Fill
import love.yinlin.compose.game.traits.AABB

@Stable
abstract class Spirit(val manager: Manager) {
    abstract val box: AABB
    open val preTransform: (DrawTransform.() -> Unit)? = null
    abstract fun Drawer.onDraw()

    fun Drawer.draw() {
        transform({
            translate(box.topLeft)
            preTransform?.invoke(this)
        }) {
            onDraw()
        }
    }

    fun Drawer.circle(color: Color, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = circle(color, box.center, box.radius, alpha, style, blendMode)
    fun Drawer.circle(brush: Brush, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = circle(brush, box.center, box.radius, alpha, style, blendMode)
    fun Drawer.rect(color: Color, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = rect(color, box.topLeft, box.size, alpha, style, blendMode)
    fun Drawer.rect(brush: Brush, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = rect(brush, box.topLeft, box.size, alpha, style, blendMode)
    fun Drawer.roundRect(color: Color, radius: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = roundRect(color, radius, box.topLeft, box.size, alpha, style, blendMode)
    fun Drawer.roundRect(brush: Brush, radius: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = roundRect(brush, radius, box.topLeft, box.size, alpha, style, blendMode)
    fun Drawer.arc(color: Color, startAngle: Float, sweepAngle: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = arc(color, startAngle, sweepAngle, box.topLeft, box.size, alpha, style, blendMode)
    fun Drawer.arc(brush: Brush, startAngle: Float, sweepAngle: Float, alpha: Float = 1f, style: DrawStyle = Fill, blendMode: BlendMode = BlendMode.SrcOver) = arc(brush, startAngle, sweepAngle, box.topLeft, box.size, alpha, style, blendMode)
    fun Drawer.image(image: ImageBitmap, alpha: Float = 1f, blendMode: BlendMode = BlendMode.SrcOver) = image(image, box.topLeft, box.size, alpha, blendMode)
    fun Drawer.clip(block: Drawer.() -> Unit) = clip(box.topLeft, box.size, block)
}