package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntSize

@Stable
class Camera {
    // 脏区标记
    internal var requireDirty: Long by mutableLongStateOf(0L)
        private set

    /**
     * 视口大小
     */
    internal var viewportSize: Size = Size.Zero
        private set

    /**
     * 原始视口缩放
     */
    private var rawViewportScale: Float = 1f

    /**
     * 相机位置
     *
     * 相机的位置是以视口中心点为锚点
     */
    var position: Offset = Offset.Zero
        set(value) {
            if (field != value) {
                field = value
                updateBounds()
                ++requireDirty
            }
        }

    /**
     * 相机缩放
     *
     * 相机的缩放是以视口中心点为锚点
     */
    var scale: Float = 1f
        set(value) {
            if (field != value) {
                field = value
                updateBounds()
                ++requireDirty
            }
        }

    /**
     * 视口边界大小
     */
    var viewportBoundSize: Size = Size.Zero
        private set

    /**
     * 视口边界
     */
    var viewportBounds: Rect = Rect.Zero
        private set

    private fun updateBounds() {
        val boundSize = viewportSize / scale
        val (x, y) = position
        val (w, h) = boundSize
        viewportBoundSize = boundSize
        viewportBounds = Rect(left = x - w / 2, top = y - h / 2, right = x + w / 2, bottom = y + h / 2)
    }

    internal fun updateViewport(size: IntSize, viewport: Viewport) {
        val (newSize, newScale) = viewport.applyCanvasBounds(size)
        rawViewportScale = newScale
        viewportSize = newSize
        updateBounds()
        ++requireDirty
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