package love.yinlin.compose.game.traits

import androidx.annotation.CallSuper
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import love.yinlin.compose.extension.translate
import love.yinlin.compose.extension.scale
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.common.LayerOrder
import love.yinlin.compose.game.common.PrepareDrawer
import love.yinlin.compose.game.plugin.ScenePlugin
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Stable
open class Layer(
    private val scene: ScenePlugin,
    vararg visibles: Visible,
    val layerOrder: Int = LayerOrder.Default, // 层级
    val textCacheCapacity: Int = 8, // 文本绘制缓存容量
    override val id: String = Uuid.generateV7().toString(),
): Entity(), Dynamic {
    private val items = visibles.sortedBy(Visible::layerOrder).toMutableStateList()

    /**
     * 可见
     */
    var visible by mutableStateOf(true)

    operator fun plusAssign(visible: Visible) {
        // 根据 layerOrder 二分查找
        val index = items.binarySearchBy(visible.layerOrder, selector = Visible::layerOrder)
        items.add(-index - 1, visible)
        visible.onAttached(scene.engine)
    }

    operator fun minusAssign(visible: Visible) {
        items -= visible
        visible.onDetached(scene.engine)
    }

    @CallSuper
    override fun onAttached(engine: Engine) {
        items.fastForEach { it.onAttached(engine) }
    }

    @CallSuper
    override fun onDetached(engine: Engine) {
        items.fastForEachReversed { it.onDetached(engine) }
    }

    override var active: Boolean = true

    override fun onUpdate(tick: Long) {
        val bounds = scene.camera.viewportBounds
        items.fastForEach { item ->
            if (item is Dynamic && item.active) {
                // 视口剔除
                val culling = if (item.culling) {
                    val (x, y) = item.position
                    val s = item.size
                    val radius = (s.width + s.height) * item.scale / 2
                    when {
                        x + radius < bounds.left -> true
                        x - radius > bounds.right -> true
                        y + radius < bounds.top -> true
                        y - radius > bounds.bottom -> true
                        else -> false
                    }
                } else false
                if (!culling) item.onUpdate(tick)
            }
        }
    }

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

    internal fun Drawer.drawVisibleLayer(rawScope: DrawScope, bounds: Rect) {
        items.fastForEach { item ->
            val _ = item.requireDirty

            // 视口剔除
            val (x, y) = item.position
            val sc = item.scale
            val s = item.size
            val culling = if (!item.visible) true else if (!item.culling) false else {
                val radius = (s.width + s.height) * sc / 2
                when {
                    x + radius < bounds.left -> true
                    x - radius > bounds.right -> true
                    y + radius < bounds.top -> true
                    y - radius > bounds.bottom -> true
                    else -> false
                }
            }
            val rt = item.rotate

            if (!culling) {
                rawScope.withTransform({
                    // 偏移
                    translate(x, y)
                    // 旋转
                    if (rt != 0f) rotate(degrees = rt, pivot = Offset.Zero)
                    // 缩放
                    if (sc != 1f) scale(ratio = sc, pivot = Offset.Zero)
                    // Canvas偏移
                    translate(-item.center)
                    // 裁切
                    if (item.clip) item.shape.onClip(this, s)
                }) {
                    with(item) { onDraw() }
                }
            }
        }
    }
}