package love.yinlin.compose.game.traits

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import love.yinlin.compose.extension.translate
import love.yinlin.compose.extension.scale
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.event.Event
import love.yinlin.compose.game.drawer.LayerOrder
import love.yinlin.compose.game.drawer.PrepareDrawer
import love.yinlin.compose.game.plugin.ScenePlugin

@Stable
open class Layer(
    vararg visibles: Visible,
    val layerOrder: Int = LayerOrder.Default, // 层级
): Entity(), Dynamic {
    private val items = visibles.sortedBy(Visible::layerOrder).toMutableList()

    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()
    val visibleCount: Int get() = items.size

    var scene: ScenePlugin? = null
        private set

    /**
     * 可见
     */
    var visible: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateDirty()
            }
        }

    operator fun plusAssign(item: Visible) {
        // 根据 layerOrder 二分查找
        val index = items.binarySearchBy(item.layerOrder, selector = Visible::layerOrder)
        items.add(if (index < 0) -index - 1 else index, item)
        item.layer = this
        updateDirty()
    }

    operator fun plusAssign(targetItems: Iterable<Visible>) {
        for (item in targetItems) {
            val index = items.binarySearchBy(item.layerOrder, selector = Visible::layerOrder)
            items.add(if (index < 0) -index - 1 else index, item)
            item.layer = this
        }
        updateDirty()
    }

    operator fun minusAssign(item: Visible) {
        items -= item
        item.layer = null
        updateDirty()
    }

    operator fun minusAssign(targetItems: Iterable<Visible>) {
        for (item in targetItems) {
            items -= item
            item.layer = null
        }
        updateDirty()
    }

    protected open fun onLayerAttached(scene: ScenePlugin) { }

    protected open fun onLayerDetached(sender: ScenePlugin) { }

    final override fun onAttached(scene: ScenePlugin) {
        this.scene = scene
        items.fastForEach { it.layer = this }
        onLayerAttached(scene)
    }

    final override fun onDetached(scene: ScenePlugin) {
        onLayerDetached(scene)
        items.clear()
        items.fastForEachReversed { it.layer = null }
        this.scene = null
    }

    /**
     * 动态激活
     */
    override var active: Boolean = true

    open fun preUpdate(tick: Int) { }

    final override fun onUpdate(tick: Int) {
        preUpdate(tick)
        items.fastForEach { item ->
            if (item is Dynamic && item.active && item.alive) item.onUpdate(tick)
        }
    }

    /**
     * 可交互触发事件
     */
    open val interactive: Boolean = true

    open fun preTrigger(tick: Int, event: Event): Boolean = false

    internal fun triggerVisibleLayer(tick: Int, event: Event): Boolean {
        if (preTrigger(tick, event)) return true
        // 事件处理层级逆向
        for (index in items.indices.reversed()) {
            val item = items[index]
            val trigger = item.trigger ?: continue
            if (trigger.onEvent(tick, event, item)) return true // 消费完成
        }
        return false
    }

    // 绘制预处理
    internal fun prepareDrawVisibleLayer(drawer: PrepareDrawer, viewportSize: Size, bounds: Rect) {
        items.fastForEach { item ->
            // 更新视口剔除
            item.updateCulling(bounds)
            // 检查视口剔除
            if (item.alive) {
                with(item) {
                    drawer.prepareDraw(viewportSize, bounds)
                }
            }
        }
    }

    // 绘制
    internal fun drawVisibleLayer(drawer: Drawer) {
        items.fastForEach { item ->
            // 检查视口剔除
            if (item.alive) {
                drawer.transform({
                    // 偏移
                    translate(item.position)
                    // 旋转
                    item.rotate.let { if (it != 0f) rotate(degrees = it, pivot = Offset.Zero) }
                    // 缩放
                    item.scale.let { if (it != 1f) scale(ratio = it, pivot = Offset.Zero) }
                    // Canvas偏移
                    translate(-item.center)
                    // 裁切
                    if (item.clip) item.aabb.onClip(this, item.size)
                }) {
                    with(item) { onDraw() }
                }
            }
        }
    }

    // 脏区标记
    private var dirtyValue: Long by mutableLongStateOf(0L)

    internal inline fun <R> whenDirty(block: () -> R): R {
        val _ = dirtyValue
        return block()
    }

    internal fun updateDirty() {
        ++dirtyValue
    }
}