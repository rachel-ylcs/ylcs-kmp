package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import love.yinlin.compose.game.Viewport

@Stable
class Camera {
    /**
     * 视口大小
     */
    internal var viewportSize: Size by mutableStateOf(Size.Zero)
        private set

    /**
     * 原始视口缩放
     */
    private var rawViewportScale: Float by mutableFloatStateOf(1f)

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
     * 视口边界大小
     */
    val viewportBoundSize: Size get() = viewportSize / scale

    /**
     * 视口边界
     */
    val viewportBounds: Rect get() {
        val (x, y) = position
        val (w, h) = viewportBoundSize
        return Rect(left = x - w / 2, top = y - h / 2, right = x + w / 2, bottom = y + h / 2)
    }

    inline fun updatePosition(block: (Offset) -> Offset) { position = block(position) }

    internal fun updateViewport(size: Size, viewport: Viewport) {
        val (newSize, newScale) = viewport.applyCanvasBounds(size)
        viewportSize = newSize
        rawViewportScale = newScale
    }

    internal fun transformLayer(scope: GraphicsLayerScope, size: Size) {
        val totalScale = rawViewportScale * scale
        val (centerX, centerY) = size / 2f
        val (cameraX, cameraY) = position * totalScale

        scope.transformOrigin = TransformOrigin(0f, 0f)
        scope.scaleX = totalScale
        scope.scaleY = totalScale
        scope.translationX = centerX - cameraX
        scope.translationY = centerY - cameraY
    }
}