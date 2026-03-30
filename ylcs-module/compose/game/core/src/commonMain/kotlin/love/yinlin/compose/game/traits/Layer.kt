package love.yinlin.compose.game.traits

import androidx.annotation.CallSuper
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import love.yinlin.compose.extension.translate
import love.yinlin.compose.extension.scale
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.event.Event
import love.yinlin.compose.game.common.LayerOrder
import love.yinlin.compose.game.common.PrepareDrawer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Stable
open class Layer(
    vararg visibles: Visible,
    val layerOrder: Int = LayerOrder.Default, // 层级
    override val id: String = Uuid.generateV7().toString(),
): Entity(), Dynamic {
    private var engine: Engine? = null

    private val items = visibles.sortedBy(Visible::layerOrder).toMutableStateList()

    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()
    val visibleCount: Int get() = items.size

    // 脏区标记
    internal var requireDirty: Long by mutableLongStateOf(0L)
        private set

    /**
     * 可见
     */
    var visible: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                ++requireDirty
            }
        }

    operator fun plusAssign(item: Visible) {
        engine?.let {
            // 根据 layerOrder 二分查找
            val index = items.binarySearchBy(item.layerOrder, selector = Visible::layerOrder)
            items.add(-index - 1, item)
            item.onAttached(it)
        }
    }

    operator fun minusAssign(item: Visible) {
        engine?.let {
            items -= item
            item.onDetached(it)
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
     * 动态激活
     */
    override var active: Boolean = true

    override fun onUpdate(tick: Long) {
        items.fastForEach { item ->
            if (item is Dynamic && item.active && item.needReDraw) item.onUpdate(tick)
        }
    }

    /**
     * 可交互触发事件
     */
    open val interactive: Boolean = true

    internal fun triggerVisibleLayer(tick: Long, event: Event): Boolean {
        // 事件处理层级逆向
        for (index in items.indices.reversed()) {
            val item = items[index]
            val trigger = item.trigger ?: continue
            if (trigger.onEvent(tick, event)) return true // 消费完成
        }
        return false
    }

    internal fun PrepareDrawer.prepareDrawVisibleLayer(viewportSize: Size, bounds: Rect) {
        items.fastForEach { item ->
            val _ = item.requireDirty

            // 视口剔除
            val (x, y) = item.position
            val s = item.size
            val needReDraw = if (!item.visible) false else if (!item.culling) true else {
                val radius = (s.width + s.height) * item.scale / 2
                when {
                    x + radius < bounds.left -> false
                    x - radius > bounds.right -> false
                    y + radius < bounds.top -> false
                    y - radius > bounds.bottom -> false
                    else -> true
                }
            }

            item.needReDraw = needReDraw

            // 需要重绘
            if (needReDraw) {
                with(item) {
                    prepareDraw(viewportSize, bounds)
                }
            }
        }
    }

    internal fun Drawer.drawVisibleLayer() {
        items.fastForEach { item ->
            if (item.needReDraw) {
                transform({
                    // 偏移
                    translate(item.position)
                    // 旋转
                    item.rotate.let { if (it != 0f) rotate(degrees = it, pivot = Offset.Zero) }
                    // 缩放
                    item.scale.let { if (it != 1f) scale(ratio = it, pivot = Offset.Zero) }
                    // Canvas偏移
                    translate(-item.center)
                    // 裁切
                    if (item.clip) item.shape.onClip(this, item.size)
                }) {
                    with(item) { onDraw() }
                }
            }
        }
    }
}