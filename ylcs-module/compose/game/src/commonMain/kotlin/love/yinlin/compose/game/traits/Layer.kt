package love.yinlin.compose.game.traits

import androidx.annotation.CallSuper
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import love.yinlin.compose.extension.scale
import love.yinlin.compose.extension.translate
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.common.LayerOrder
import love.yinlin.compose.game.common.PrepareDrawer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Stable
open class Layer(
    vararg visibles: Visible,
    val layerOrder: Int = LayerOrder.Default, // 层级
    val textCacheCapacity: Int = 8, // 文本绘制缓存容量
    override val id: String = Uuid.generateV7().toString(),
): Entity() {
    private var engine: Engine? = null

    private val items = mutableStateListOf(*visibles)

    private val visibleItems by derivedStateOf {
        items.fastFilter { it.visible }.sortedBy(Visible::layerOrder)
    }

    /**
     * 可见
     */
    var visible by mutableStateOf(true)

    operator fun plusAssign(visible: Visible) {
        engine?.let {
            items += visible
            visible.onAttached(it)
        }
    }

    operator fun minusAssign(visible: Visible) {
        engine?.let {
            items -= visible
            visible.onDetached(it)
        }
    }

    @CallSuper
    override fun onAttached(engine: Engine) {
        this.engine = engine
        items.fastForEach { it.onAttached(engine) }
    }

    @CallSuper
    override fun onDetached(engine: Engine) {
        items.fastForEachReversed { it.onDetached(engine) }
        this.engine = null
    }

    /**
     * 预绘制处理
     *
     * 此处不允许使用Drawer绘制内容，只能测量与更新。
     * 注意预处理和绘制只需要使用普通变量即可，它们位于同一个作用域下不需要状态监听。
     *
     * @param viewportSize 视口大小 (等价于设计稿, 不包括相机缩放)
     * @param viewportBounds 视口边界 (实际上屏幕能看到的视口范围)
     *
     */
    open fun PrepareDrawer.prepareDraw(viewportSize: Size, viewportBounds: Rect) { }

    internal fun Drawer.drawVisibleLayer(rawScope: DrawScope, bounds: Rect) {
        visibleItems.fastForEach { item ->
            // 视口剔除
            val (x, y) = item.position
            val s = item.scale
            val size = item.size
            val culling = if (!item.culling) false else {
                val radius = (size.width + size.height) * s / 2
                when {
                    x + radius < bounds.left -> true
                    x - radius > bounds.right -> true
                    y + radius < bounds.top -> true
                    y - radius > bounds.bottom -> true
                    else -> false
                }
            }
            if (!culling) {
                rawScope.withTransform({
                    // 偏移
                    translate(x, y)
                    // 旋转
                    if (item.rotate != 0f) rotate(degrees = item.rotate, pivot = Offset.Zero)
                    // 缩放
                    if (item.scale != 1f) scale(ratio = s, pivot = Offset.Zero)
                    // Canvas偏移
                    translate(-item.center)
                    // 裁切
                    if (item.clip) item.shape.onClip(this, size)
                }) {
                    with(item) { onDraw() }
                }
            }
        }
    }
}