package love.yinlin.compose.game.traits

import androidx.annotation.CallSuper
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import love.yinlin.compose.extension.scale
import love.yinlin.compose.extension.translate
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.Drawer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Stable
open class Layer(
    vararg visibles: Visible,
    val zIndex: Int = 0, // 层级
    override val id: String = Uuid.generateV7().toString(),
): Entity() {
    companion object {
        // 视口剔除
        private fun requireCulling(bounds: Rect, visible: Visible): Boolean {
            if (!visible.culling) return false
            val (x, y) = visible.position
            val (w, h) = visible.size
            val radius = (w + h) * visible.scale / 2
            if (x + radius < bounds.left) return true
            if (x - radius > bounds.right) return true
            if (y + radius < bounds.top) return true
            if (y - radius > bounds.bottom) return true
            return false
        }
    }

    private var engine: Engine? = null

    private val items = mutableStateListOf(*visibles)

    private val visibleItems by derivedStateOf {
        items.fastFilter { it.visible }.sortedBy(Visible::zIndex)
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

    internal fun Drawer.drawVisibleLayer(viewportSize: Size, bounds: Rect) {
        visibleItems.fastForEach { item ->
            // 视口剔除
            if (!requireCulling(bounds, item)) {
                scope?.withTransform({
                    // 偏移
                    translate(offset = item.position)
                    // 旋转
                    if (item.rotate != 0f) rotate(degrees = item.rotate, pivot = Offset.Zero)
                    // 缩放
                    if (item.scale != 1f) scale(ratio = item.scale, pivot = Offset.Zero)
                    // Canvas偏移
                    translate(-item.center)
                    // 裁切
                    if (item.clip) item.shape.onClip(this, item.size)
                }) {
                    with(item) { onDraw(viewportSize) }
                }
            }
        }
    }
}