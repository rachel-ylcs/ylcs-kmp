package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import love.yinlin.compose.extension.mutableOffsetStateOf
import love.yinlin.compose.extension.mutableSizeStateOf

@Stable
open class Body(
    position: Offset = Offset.Zero,
    size: Size = Size.Zero,
) : Entity() {
    /**
     * 是否裁切溢出
     */
    open val clip: Boolean = true

    /**
     * 形状
     */
    open val shape: Shape = Shape.Box

    /**
     * 位置
     */
    var position: Offset by mutableOffsetStateOf(position)

    /**
     * 大小
     */
    var size: Size by mutableSizeStateOf(size)

    /**
     * 缩放
     */
    var scale: Float by mutableFloatStateOf(1f)

    /**
     * 旋转
     */
    var rotate: Float by mutableFloatStateOf(0f)

    /**
     * 中心点
     */
    val center: Offset get() = size.center
}