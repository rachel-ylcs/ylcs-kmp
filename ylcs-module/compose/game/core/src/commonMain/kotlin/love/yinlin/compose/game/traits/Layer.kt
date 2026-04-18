package love.yinlin.compose.game.traits

import androidx.compose.runtime.*
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import love.yinlin.compose.extension.translate
import love.yinlin.compose.extension.scale
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.InitialDrawer
import love.yinlin.compose.game.event.Event
import love.yinlin.compose.game.drawer.LayerOrder
import love.yinlin.compose.game.drawer.LayerType
import love.yinlin.compose.game.drawer.PrepareDrawer
import love.yinlin.compose.game.plugin.ScenePlugin

@Stable
open class Layer(
    vararg visibles: Visible,
    val layerOrder: Int = LayerOrder.Default, // 层级
    val layerType: LayerType = LayerType.Relative, // 层类型
) : Entity(), Dynamic {
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
        item.onVisibleAttached(this)
        updateDirty()
    }

    operator fun plusAssign(targetItems: Iterable<Visible>) {
        for (item in targetItems) {
            val index = items.binarySearchBy(item.layerOrder, selector = Visible::layerOrder)
            items.add(if (index < 0) -index - 1 else index, item)
            item.onVisibleAttached(this)
        }
        updateDirty()
    }

    operator fun minusAssign(item: Visible) {
        items -= item
        item.onVisibleDetached()
        updateDirty()
    }

    operator fun minusAssign(targetItems: Iterable<Visible>) {
        for (item in targetItems) {
            items -= item
            item.onVisibleDetached()
        }
        updateDirty()
    }

    protected open fun onLayerAttached(scene: ScenePlugin) { }

    protected open fun onLayerDetached(sender: ScenePlugin) { }

    final override fun onAttached(scene: ScenePlugin) {
        this.scene = scene
        items.fastForEach { it.onVisibleAttached(this) }
        onLayerAttached(scene)
    }

    final override fun onDetached(scene: ScenePlugin) {
        onLayerDetached(scene)
        items.clear()
        items.fastForEachReversed { it.onVisibleDetached() }
        this.scene = null
    }

    /**
     * 动态激活
     */
    override var active: Boolean = true

    /**
     * 预更新
     */
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

    /**
     * 预受击测试
     */
    open fun preHitTest(point: Offset): Any? = null

    /**
     * 预事件处理
     */
    open val preTrigger: Trigger? = null

    internal fun hitTestVisibleLayer(point: Offset): Pair<Visible?, Any>? {
        val layerArg = preHitTest(point)
        if (layerArg != null) return null to layerArg
        // 受击处理层级逆向
        for (index in items.indices.reversed()) {
            val item = items[index]
            val arg = item.onHitTest(point)
            if (arg != null) return item to arg
        }
        return null
    }

    internal fun triggerVisibleLayer(event: Event): Boolean = when (event) {
        // 检查是否是指针事件 拦截
        is Event.Pointer -> {
            if (event.layer != this) false
            else {
                val source = event.source
                if (source == null) preTrigger?.onEvent(event, null)
                else source.trigger?.onEvent(event, source)
                // 即使没有触发器，受击检测通过就必须消费完成
                true
            }
        }
        // 其他事件
        else -> {
            if (preTrigger?.onEvent(event, null) == true) true
            else {
                // 事件处理层级逆向
                for (index in items.indices.reversed()) {
                    val source = items[index]
                    val trigger = source.trigger ?: continue
                    if (trigger.onEvent(event, source)) return true // 消费完成
                }
                false
            }
        }
    }

    /**
     * 初始化绘制
     */
    open suspend fun InitialDrawer.preInitialDraw() { }

    /**
     * 预准备绘制
     */
    open fun PrepareDrawer.prePrepareDraw(viewportSize: Size, viewportBounds: Rect) { }

    /**
     * 预绘制
     */
    open fun Drawer.preOnDraw() { }

    private fun Drawer.drawVisibleRelative(item: Visible) {
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
            if (item.clip) item.aabb.onClip(this, item.size)
        }) {
            with(item) { onDraw() }
        }
    }

    private fun Drawer.drawVisibleAbsolute(item: Visible) {
        transform({
            // 偏移
            translate(item.position)
            // 旋转
            item.rotate.let { if (it != 0f) rotate(degrees = it, pivot = Offset.Zero) }
            // 缩放
            item.scale.let { if (it != 1f) scale(ratio = it, pivot = Offset.Zero) }
        }) {
            with(item) { onDraw() }
        }
    }

    internal fun drawCacheVisibleLayerRelative(scope: CacheDrawScope, drawer: Drawer, viewportSize: Size, bounds: Rect): DrawResult {
        return whenDirty {
            if (visible) {
                // 绘制预处理
                drawer.withRawCacheScope(scope) {
                    prePrepareDraw(viewportSize, bounds)

                    // 遍历元素
                    items.fastForEach { item ->
                        // 更新视口剔除
                        item.updateCulling(bounds)
                        // 检查视口剔除
                        if (item.alive) {
                            with(item) {
                                prepareDraw(viewportSize, bounds)
                            }
                        }
                    }
                }
            }

            // 绘制
            scope.onDrawWithContent {
                if (visible) {
                    drawer.withRawScope(this) {
                        preOnDraw()

                        items.fastForEach { item ->
                            // 检查视口剔除
                            if (item.alive) drawVisibleRelative(item)
                        }
                    }
                }
            }
        }
    }

    internal fun drawCacheVisibleLayerAbsolute(scope: CacheDrawScope, drawer: Drawer, viewportSize: Size): DrawResult {
        return whenDirty {
            if (visible) {
                // 绘制预处理
                drawer.withRawCacheScope(scope) {
                    val viewportBounds = Rect(Offset.Zero, viewportSize)
                    prePrepareDraw(viewportSize, viewportBounds)

                    items.fastForEach { item ->
                        with(item) {
                            prepareDraw(viewportSize, viewportBounds)
                        }
                    }
                }
            }

            // 绘制
            scope.onDrawWithContent {
                if (visible) {
                    drawer.withRawScope(this) {
                        preOnDraw()

                        items.fastForEach { drawVisibleAbsolute(it) }
                    }
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

    fun updateDirty() { ++dirtyValue }
}