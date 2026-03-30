package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.common.LayerOrder
import love.yinlin.compose.game.common.PrepareDrawer

@Stable
abstract class Visible(
    position: Offset = Offset.Zero,
    size: Size = Size.Zero,
    aabb: AABB = AABB.Box,
    visible: Boolean = true
) : Entity() {
    /**
     * 是否裁切溢出
     */
    open val clip: Boolean = true

    /**
     * 层级
     */
    open val layerOrder: Int = LayerOrder.Default

    /**
     * 参与视口剔除
     */
    open val culling: Boolean = true

    /**
     * 触发器
     */
    open val trigger: Trigger? = null

    /**
     * 预绘制处理
     *
     * 此处不允许使用Drawer绘制内容，只能测量并更新非脏区值。
     * 注意预处理和绘制只需要使用普通变量即可，它们位于同一个作用域下不需要状态监听。
     *
     * @param viewportSize 视口大小 (等价于设计稿, 不包括相机缩放)
     * @param viewportBounds 视口边界 (实际上屏幕能看到的视口范围)
     *
     */
    open fun PrepareDrawer.prepareDraw(viewportSize: Size, viewportBounds: Rect) { }

    internal var needReDraw: Boolean = true

    /**
     * 绘制
     */
    abstract fun Drawer.onDraw()

    // 脏区标记
    internal var requireDirty: Long by mutableLongStateOf(0L)
        private set

    /**
     * 碰撞箱
     */
    var aabb: AABB = aabb
        set(value) {
            if (value != field) {
                field = value
                ++requireDirty
            }
        }

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

    /**
     * 半径
     */
    val minDimension: Float get() = size.minDimension
    val maxDimension: Float get() = size.maxDimension

    /**
     * 边界
     */
    val bounds: Rect get() = Rect(Offset.Zero, size)

    /**
     * 角点
     */
    val topLeft: Offset get() = Offset.Zero
    val topCenter: Offset get() = Offset(size.width / 2, 0f)
    val topRight: Offset get() = Offset(size.width, 0f)
    val bottomLeft: Offset get() = Offset(0f, size.height)
    val bottomCenter: Offset get() = Offset(size.width / 2, size.height)
    val bottomRight: Offset get() = Offset(size.width, size.height)
    val centerLeft: Offset get() = Offset(0f, size.height / 2)
    val centerRight: Offset get() = Offset(size.width, size.height / 2)
}