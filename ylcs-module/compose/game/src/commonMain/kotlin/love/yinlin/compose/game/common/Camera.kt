package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.Viewport

@Stable
class Camera {
    /**
     * 原始视口大小
     */
    internal var rawViewportSize: Size by mutableStateOf(Size.Zero)
        private set

    /**
     * 原始视口缩放
     */
    internal var rawViewportScale: Float by mutableFloatStateOf(1f)
        private set

    /**
     * 相机位置
     *
     * 相机的位置是以视口中心点为锚点
     */
    var position: Offset by mutableStateOf(Offset.Zero)

    /**
     * 相机缩放
     *
     * 相机的缩放是以视口中心点为锚点
     */
    var scale: Float by mutableStateOf(1f)

    /**
     * 视口大小
     */
    val viewportSize: Size get() = rawViewportSize / scale

    /**
     * 视口边界 AABB
     */
    val viewportBounds: Rect get() {
        val (x, y) = position
        val (w, h) = rawViewportSize / scale
        return Rect(left = x - w / 2, top = y - h / 2, right = x + w / 2, bottom = y + h / 2)
    }

    inline fun updatePosition(block: (Offset) -> Offset) { position = block(position) }

    internal fun updateViewport(size: Size, viewport: Viewport): Camera {
        val (newSize, newScale) = viewport.applyCanvasBounds(size)
        rawViewportSize = newSize
        rawViewportScale = newScale
        return this
    }
}