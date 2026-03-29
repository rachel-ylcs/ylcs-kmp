package love.yinlin.compose.game

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
    fun applyWindowBounds(windowSize: IntSize): IntRect
    fun applyCanvasBounds(canvasSize: Size): Pair<Size, Float>

    @Stable
    data class Fixed(val width: Int, val height: Int): Viewport {
        override fun applyWindowBounds(windowSize: IntSize): IntRect {
            require(width > 0 && height > 0)
            val scaleX = windowSize.width / width.toFloat()
            val scaleY = windowSize.height / height.toFloat()
            val minScale = min(scaleX, scaleY)
            val windowWidth = (width * minScale).toInt()
            val windowHeight = (height * minScale).toInt()
            val offsetX = (windowSize.width - windowWidth) / 2
            val offsetY = (windowSize.height - windowHeight) / 2
            return IntRect(IntOffset(offsetX, offsetY), IntSize(windowWidth, windowHeight))
        }

        override fun applyCanvasBounds(canvasSize: Size): Pair<Size, Float> {
            require(width > 0)
            return Size(width.toFloat(), height.toFloat()) to canvasSize.width / width
        }
    }

    @Stable
    data class MatchWidth(val width: Int): Viewport {
        override fun applyWindowBounds(windowSize: IntSize): IntRect {
            require(width > 0)
            return IntRect(IntOffset.Zero, windowSize)
        }

        override fun applyCanvasBounds(canvasSize: Size): Pair<Size, Float> {
            require(width > 0)
            val scale = canvasSize.width / width
            return Size(width.toFloat(), canvasSize.height / scale) to scale
        }
    }

    @Stable
    data class MatchHeight(val height: Int) : Viewport {
        override fun applyWindowBounds(windowSize: IntSize): IntRect {
            require(height > 0)
            return IntRect(IntOffset.Zero, windowSize)
        }

        override fun applyCanvasBounds(canvasSize: Size): Pair<Size, Float> {
            require(height > 0)
            val scale = canvasSize.height / height
            return Size(canvasSize.width / scale, height.toFloat()) to scale
        }
    }
}