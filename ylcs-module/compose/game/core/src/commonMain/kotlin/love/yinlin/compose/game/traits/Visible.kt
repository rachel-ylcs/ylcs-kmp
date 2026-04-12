package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import love.yinlin.compose.game.viewport.Culling
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.LayerOrder
import love.yinlin.compose.game.drawer.PrepareDrawer

/**
 * 可视实体
 */
@Stable
abstract class Visible(
    position: Offset = Offset.Zero,
    size: Size = Size.Zero,
    aabb: AABB = AABB.Box,
    visible: Boolean = true,
    useCulling: Boolean = true, // 参与视口剔除
) {
    /**
     * 是否裁切溢出
     */
    open val clip: Boolean = false

    /**
     * 层级
     */
    open val layerOrder: Int = LayerOrder.Default

    /**
     * 触发器
     */
    open val trigger: Trigger? = null

    /**
     * 附加层
     */
    protected open fun onAttached() { }

    /**
     * 离开层
     */
    protected open fun onDetached() { }

    var layer: Layer? = null
        private set

    internal fun onVisibleAttached(layer: Layer) {
        this.layer = layer
        onAttached()
    }

    internal fun onVisibleDetached() {
        onDetached()
        this.layer = null
    }

    /**
     * 受击检测
     */
    internal fun onHitTest(point: Offset): Boolean {
        // 获取相对于 visible 的位置
        val visiblePosition = point - position + center
        // 检查碰撞箱
        return aabb.contains(size, visiblePosition)
    }

    /**
     * 绘制预处理
     *
     * 脏区更新时触发，不允许使用Drawer绘制内容，只能测量并更新。
     * 注意：此处只需要使用普通变量即可，不可以更新自身属性。
     *
     * @param viewportSize 视口大小 (等价于设计稿, 不包括相机缩放)
     * @param viewportBounds 视口边界 (实际上屏幕能看到的视口范围)
     *
     */
    open fun PrepareDrawer.prepareDraw(viewportSize: Size, viewportBounds: Rect) { }

    /**
     * 绘制
     *
     * 脏区更新时触发，只能绘制内容。
     * 注意：绘制可以直接使用预处理保存的普通变量，不可以更新自身属性。
     */
    abstract fun Drawer.onDraw()

    // 视口剔除处理
    private val culling: Culling? = if (useCulling) Culling() else null

    internal val alive: Boolean get() = culling?.enabled != true

    internal fun updateCulling(bounds: Rect) {
        // 计算与视口剔除相关的属性是否发生变化
        val isDirty = culling?.let { it.dirty || bounds != it.bounds } ?: return
        // 如果没变化使用缓存值
        if (isDirty) {
            // 视口剔除
            val (x, y) = position
            // 更新缓存值
            culling.enabled = if (!visible) true else { // 不可视直接剔除
                // 计算包围盒半径 弱条件
                val radius = (size.width + size.height) * scale / 2
                when {
                    x + radius < bounds.left -> true
                    x - radius > bounds.right -> true
                    y + radius < bounds.top -> true
                    y - radius > bounds.bottom -> true
                    else -> false
                }
            }
            // 重置dirty
            culling.dirty = false
        }
    }

    /**
     * 碰撞箱
     */
    var aabb: AABB = aabb
        set(value) {
            if (value != field) {
                field = value
                layer?.updateDirty()
            }
        }

    /**
     * 位置
     */
    var position: Offset = position
        set(value) {
            if (value != field) {
                field = value
                culling?.dirty = true
                layer?.updateDirty()
            }
        }

    /**
     * 大小
     */
    var size: Size = size
        set(value) {
            if (value != field) {
                field = value
                culling?.dirty = true
                layer?.updateDirty()
            }
        }

    /**
     * 缩放
     */
    var scale: Float = 1f
        set(value) {
            if (value != field) {
                field = value
                culling?.dirty = true
                layer?.updateDirty()
            }
        }

    /**
     * 旋转
     */
    var rotate: Float = 0f
        set(value) {
            if (value != field) {
                field = value
                layer?.updateDirty()
            }
        }

    /**
     * 可见
     */
    var visible: Boolean = visible
        set(value) {
            if (field != value) {
                field = value
                culling?.dirty = true
                layer?.updateDirty()
            }
        }

    fun updateDirty() { layer?.updateDirty() }

    //  ---  计算属性  ---

    /**
     * 中心点
     */
    val center: Offset get() = size.center

    // 半径
    val minDimension: Float get() = size.minDimension
    val maxDimension: Float get() = size.maxDimension

    // 边界
    val bounds: Rect get() = Rect(Offset.Zero, size)

    // 角点
    val topLeft: Offset get() = Offset.Zero
    val topCenter: Offset get() = Offset(size.width / 2, 0f)
    val topRight: Offset get() = Offset(size.width, 0f)
    val bottomLeft: Offset get() = Offset(0f, size.height)
    val bottomCenter: Offset get() = Offset(size.width / 2, size.height)
    val bottomRight: Offset get() = Offset(size.width, size.height)
    val centerLeft: Offset get() = Offset(0f, size.height / 2)
    val centerRight: Offset get() = Offset(size.width, size.height / 2)
}