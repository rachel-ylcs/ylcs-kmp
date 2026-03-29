package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import love.yinlin.compose.extension.mutableOffsetStateOf
import love.yinlin.compose.extension.mutableSizeStateOf
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.common.LayerOrder

@Stable
abstract class Visible(
    position: Offset = Offset.Zero,
    size: Size = Size.Zero,
    visible: Boolean = true
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
     * 层级
     */
    open val layerOrder: Int = LayerOrder.Default

    /**
     * 参与视口剔除
     */
    open val culling: Boolean = true

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
     * 可见
     */
    var visible by mutableStateOf(visible)

    /**
     * 绘制
     */
    abstract fun Drawer.onDraw()

    /**
     * 中心点
     */
    val center: Offset get() = size.center
}