package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
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
     * 绘制
     */
    abstract fun Drawer.onDraw()

    // 脏区标记
    internal var requireDirty: Long by mutableLongStateOf(0L)
        private set

    /**
     * 位置
     */
    var position: Offset = position
        set(value) {
            if (value != field) {
                field = value
                ++requireDirty
            }
        }

    /**
     * 大小
     */
    var size: Size = size
        set(value) {
            if (value != field) {
                field = value
                ++requireDirty
            }
        }

    /**
     * 缩放
     */
    var scale: Float = 1f
        set(value) {
            if (value != field) {
                field = value
                ++requireDirty
            }
        }

    /**
     * 旋转
     */
    var rotate: Float = 0f
        set(value) {
            if (value != field) {
                field = value
                ++requireDirty
            }
        }

    /**
     * 可见
     */
    var visible: Boolean = visible
        set(value) {
            if (field != value) {
                field = value
                ++requireDirty
            }
        }

    /**
     * 中心点
     */
    val center: Offset get() = size.center
}