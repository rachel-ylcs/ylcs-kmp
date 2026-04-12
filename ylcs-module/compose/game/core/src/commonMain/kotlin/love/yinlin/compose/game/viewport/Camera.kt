package love.yinlin.compose.game.viewport

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.extension.mutableSizeStateOf
import kotlin.math.abs
import kotlin.math.exp

@Stable
class Camera internal constructor(private val config: Config) {
    @Stable
    data class Config(
        /**
         * 相机移位缓动因子
         */
        val moveSmoothness: Float = 0.005f,
        /**
         * 相机缩放缓动因子
         */
        val scaleSmoothness: Float = 0.008f,
    )

    /**
     * 视口大小
     */
    var viewportSize: Size by mutableSizeStateOf(Size.Zero)
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
    var position: Offset = Offset.Zero
        private set

    private var targetPosition: Offset = Offset.Unspecified

    /**
     * 更新相机位置
     */
    fun updatePosition(newPosition: Offset) {
        if (newPosition != position) {
            targetPosition = Offset.Unspecified
            position = newPosition
            updateDirty()
        }
    }

    /**
     * 缓动更新位置
     */
    fun animateUpdatePosition(newPosition: Offset) { targetPosition = newPosition }

    /**
     * 相机缩放
     *
     * 相机的缩放是以视口中心点为锚点
     */
    var scale: Float = 1f
        private set

    private var targetScale: Float = Float.NaN

    /**
     * 更新相机缩放
     */
    fun updateScale(newScale: Float) {
        if (newScale != scale) {
            targetScale = Float.NaN
            scale = newScale
            updateDirty()
        }
    }

    /**
     * 缓动更新缩放
     */
    fun animateUpdateScale(newScale: Float) { targetScale = newScale }

    internal fun updateAnimation(deltaTime: Int) {
        var isDirty = false
        if (targetPosition.isSpecified) {
            val target = targetPosition
            val diff = target - position
            if (diff.getDistance() < 1f) {
                // 极近直接复位
                targetPosition = Offset.Unspecified
                position = target
            }
            else position += diff * (1 - exp(-deltaTime * config.moveSmoothness)) // 指数衰减
            isDirty = true
        }
        if (!targetScale.isNaN()) {
            val target = targetScale
            val diff = target - scale
            if (abs(diff) < 0.001f) {
                // 极近直接复位
                targetScale = Float.NaN
                scale = target
            }
            else scale += diff * (1 - exp(-deltaTime * config.scaleSmoothness)) // 指数衰减
            isDirty = true
        }
        if (isDirty) updateDirty()
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

    internal fun reset() {
        position = Offset.Zero
        targetPosition = Offset.Unspecified
        scale = 1f
        targetScale = Float.NaN
        viewportBoundSize = Size.Zero
        viewportBounds = Rect.Zero
    }

    internal fun updateViewport(size: IntSize, viewport: Viewport) {
        val (newSize, newScale) = viewport.applyCanvasBounds(size)
        rawViewportScale = newScale
        viewportSize = newSize
    }

    internal fun transformPointer(isAbsolute: Boolean, pointer: Offset, size: Size): Offset = if (isAbsolute) pointer / rawViewportScale else {
        val totalScale = rawViewportScale * scale
        (pointer - size.center) / totalScale + position
    }

    internal fun transformLayerRelative(scope: GraphicsLayerScope, size: Size) {
        val _ = dirtyValue
        val totalScale = rawViewportScale * scale
        val (centerX, centerY) = size / 2f
        val (cameraX, cameraY) = position * totalScale

        scope.transformOrigin = TransformOrigin(0f, 0f)
        scope.scaleX = totalScale
        scope.scaleY = totalScale
        scope.translationX = centerX - cameraX
        scope.translationY = centerY - cameraY
    }

    internal fun transformLayerAbsolute(scope: GraphicsLayerScope) {
        scope.transformOrigin = TransformOrigin(0f, 0f)
        scope.scaleX = rawViewportScale
        scope.scaleY = rawViewportScale
    }

    // 脏区标记
    private var dirtyValue: Long by mutableLongStateOf(0L)

    internal inline fun <R> whenDirty(block: (Size, Rect) -> R): R {
        val _ = dirtyValue
        return block(viewportSize, viewportBounds)
    }

    private fun updateDirty() {
        val boundSize = viewportSize / scale
        val (x, y) = position
        val (w, h) = boundSize
        viewportBoundSize = boundSize
        viewportBounds = Rect(left = x - w / 2, top = y - h / 2, right = x + w / 2, bottom = y + h / 2)
        ++dirtyValue
    }
}