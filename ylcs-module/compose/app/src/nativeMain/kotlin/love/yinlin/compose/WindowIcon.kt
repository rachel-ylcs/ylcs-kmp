package love.yinlin.compose

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.use
import org.jetbrains.skiko.window.NativeWindowScope
import kotlin.math.max

internal fun NativeWindowScope.setIcon(painter: Painter, density: Density, direction: LayoutDirection): Boolean {
    return setIcon(256, 256, painter.toWindowIconPixels(density, direction))
}

internal fun Painter.toWindowIconPixels(density: Density, direction: LayoutDirection, edge: Int = 256): IntArray {
    require(edge in 1 .. 1024)
    val painter = this
    val intrinsic = intrinsicSize
    val size = if (intrinsic.width.isFinite() && intrinsic.height.isFinite() && intrinsic.width > 0f && intrinsic.height > 0f) {
        val scale = edge / max(intrinsic.width, intrinsic.height)
        Size(intrinsic.width * scale, intrinsic.height * scale)
    }
    else Size(edge.toFloat(), edge.toFloat())
    return Surface.makeRasterN32Premul(edge, edge).use { surface ->
        surface.canvas.clear(Color.TRANSPARENT)
        CanvasDrawScope().draw(density, direction, surface.canvas.asComposeCanvas(), Size(edge.toFloat(), edge.toFloat())) {
            translate((edge - size.width) / 2f, (edge - size.height) / 2f) {
                with(painter) { draw(size) }
            }
        }
        Bitmap().use { bitmap ->
            check(bitmap.allocN32Pixels(edge, edge))
            check(surface.readPixels(bitmap, 0, 0)) { "Unable to rasterize the window icon" }
            val pixels = IntArray(edge * edge)
            bitmap.asComposeImageBitmap().readPixels(pixels)
            pixels
        }
    }
}
