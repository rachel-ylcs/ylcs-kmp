package love.yinlin.compose.ui.image

import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt

internal class AnimatedImagePainter(
    private val frame: State<ImageBitmap>,
    size: IntSize,
    private val quality: FilterQuality,
) : Painter() {
    override val intrinsicSize: Size = size.toSize()

    private var alpha = 1f
    private var colorFilter: ColorFilter? = null

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        drawImage(frame.value, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), alpha = alpha, colorFilter = colorFilter, filterQuality = quality)
    }
}