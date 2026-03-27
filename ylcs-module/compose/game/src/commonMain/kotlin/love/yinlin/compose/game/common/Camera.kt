package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.Viewport

@Stable
class Camera {
    /**
     * 视口缩放比例
     */
    internal var viewportScale: Float by mutableFloatStateOf(1f)
        private set

    /**
     * 视口大小
     */
    var viewportSize: Size by mutableStateOf(Size.Zero)
        private set

    /**
     * 相机偏移
     */
    var position: Offset by mutableStateOf(Offset.Zero)

    internal fun updateViewport(size: Size, viewport: Viewport) {
        val (vSize, vScale) = viewport.applyCanvasBounds(size)
        viewportSize = vSize
        viewportScale = vScale
    }
}