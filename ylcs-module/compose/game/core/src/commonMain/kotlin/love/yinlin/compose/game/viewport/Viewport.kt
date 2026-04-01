package love.yinlin.compose.game.viewport

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

/**
 * 视口
 */
@Stable
sealed interface Viewport {
    fun applyWindowBounds(outerWidth: Int, outerHeight: Int): IntRect
    fun applyCanvasBounds(canvasSize: IntSize): Pair<Size, Float>

    @Stable
    data class Fixed(val width: Int, val height: Int): Viewport {
        override fun applyWindowBounds(outerWidth: Int, outerHeight: Int): IntRect {
            require(width > 0 && height > 0)
            val scaleX = outerWidth / width.toFloat()
            val scaleY = outerHeight / height.toFloat()
            val minScale = min(scaleX, scaleY)
            val windowWidth = (width * minScale).toInt()
            val windowHeight = (height * minScale).toInt()
            val offsetX = (outerWidth - windowWidth) / 2
            val offsetY = (outerHeight - windowHeight) / 2
            return IntRect(IntOffset(offsetX, offsetY), IntSize(windowWidth, windowHeight))
        }

        override fun applyCanvasBounds(canvasSize: IntSize): Pair<Size, Float> {
            require(width > 0)
            val bound = width.toFloat()
            val scale = canvasSize.width / bound
            return Size(bound, height.toFloat()) to scale
        }
    }

    @Stable
    data class MatchWidth(val width: Int): Viewport {
        override fun applyWindowBounds(outerWidth: Int, outerHeight: Int): IntRect {
            require(width > 0)
            return IntRect(0, 0, outerWidth, outerHeight)
        }

        override fun applyCanvasBounds(canvasSize: IntSize): Pair<Size, Float> {
            require(width > 0)
            val bound = width.toFloat()
            val scale = canvasSize.width / bound
            return Size(bound, canvasSize.height / scale) to scale
        }
    }

    @Stable
    data class MatchHeight(val height: Int) : Viewport {
        override fun applyWindowBounds(outerWidth: Int, outerHeight: Int): IntRect {
            require(height > 0)
            return IntRect(0, 0, outerWidth, outerHeight)
        }

        override fun applyCanvasBounds(canvasSize: IntSize): Pair<Size, Float> {
            require(height > 0)
            val bound = height.toFloat()
            val scale = canvasSize.height / bound
            return Size(canvasSize.width / scale, bound) to scale
        }
    }
}