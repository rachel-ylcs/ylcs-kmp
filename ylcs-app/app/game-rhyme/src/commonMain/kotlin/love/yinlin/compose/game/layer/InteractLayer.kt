package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastForEachIndexed
import love.yinlin.compose.Colors
import love.yinlin.compose.extension.scale
import love.yinlin.compose.extension.translate
import love.yinlin.compose.game.common.InteractStatus
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.LayerType
import love.yinlin.compose.game.drawer.PrepareDrawer
import love.yinlin.compose.game.event.Event
import love.yinlin.compose.game.event.PointerEventListener
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.traits.Trigger
import love.yinlin.compose.game.visible.Block

// 交互层
@Stable
class InteractLayer : Layer(layerOrder = 3, layerType = LayerType.Absolute) {
    class InteractInfo(index: Int) {
        companion object {
            const val INDICATOR_RADIUS = 10f
            const val BRUSH_RADIUS = 150f
            const val BRUSH_DURATION = 80f
            val DefaultStroke = Stroke(3f)
        }

        val color: Color = if (index == 0) Colors.Transparent else Block.ScaleColorList[index]
        val brush: Brush = Brush.radialGradient(
            0f to color, 0.25f to color.copy(alpha = 0.5f), 0.5f to color.copy(alpha = 0.25f), 0.75f to color.copy(alpha = 0.05f), 1f to Colors.Transparent,
            center = Offset.Zero,
            radius = BRUSH_RADIUS
        )
        var mRect: Rect = Rect.Zero
        var currentProgress: Float = 0f
        var targetProgress: Float = 0f

        private val baseScale: Float get() = mRect.maxDimension / BRUSH_RADIUS * 1.25f

        fun Drawer.drawInteract() {
            // 画指示圈
            if (currentProgress > 0f) {
                transform({
                    translate(mRect.center)
                    scale(baseScale * currentProgress, Offset.Zero)
                }) {
                    circle(brush, Offset.Zero, BRUSH_RADIUS)
                }
            }
            // 画指示器
            roundRect(color, INDICATOR_RADIUS, mRect)
            // 画指示器边框
            roundRect(Colors.White, INDICATOR_RADIUS, mRect, style = DefaultStroke)
        }
    }

    private val infos = Array(8) { InteractInfo(it) }
    @PublishedApi internal val statusList = MutableList<InteractStatus?>(8) { null }

    private var lastViewportSize: Size = Size.Zero

    // 三等分宽度
    val w0 = 0f
    var w1: Float = 0f
    var w2: Float = 0f
    var w3: Float = 0f
    // 三等分高度
    var h0: Float = 0f
    var h1: Float = 0f
    var h2: Float = 0f
    var h3: Float = 0f

    override fun preHitTest(point: Offset): Any? {
        // 1   7
        // 2   6
        // 3 4 5
        val (x, y) = point
        return when {
            x < w0 -> null
            x < w1 -> when {
                y < h0 -> null
                y < h1 -> 1
                y < h2 -> 2
                y < h3 -> 3
                else -> null
            }
            x < w2 -> when {
                y < h2 -> null
                y < h3 -> 4
                else -> null
            }
            x < w3 -> when {
                y < h0 -> null
                y < h1 -> 7
                y < h2 -> 6
                y < h3 -> 5
                else -> null
            }
            else -> null
        }
    }

    override val preTrigger: Trigger = Trigger(
        object : PointerEventListener() {
            override fun onPointerDown(event: Event.Pointer.Down) {
                val index = event.arg as? Int ?: return
                val info = infos.getOrNull(index) ?: return
                if (statusList[index] != null) return // 轨道已经按下
                statusList[index] = InteractStatus.Down(event.id)
                info.targetProgress = 1f
            }

            override fun onPointerUp(event: Event.Pointer.Up) {
                val index = event.arg as? Int ?: return
                val info = infos.getOrNull(index) ?: return
                val id = when (val status = statusList[index]) { // 异常状态
                    null -> null
                    is InteractStatus.Down -> status.id
                    is InteractStatus.AwaitUp -> status.id
                    is InteractStatus.Up -> null
                }
                if (id != event.id) return // ID不一致
                statusList[index] = InteractStatus.Up(event.id)
                info.targetProgress = 0f
            }
        }
    )

    inline fun withInteractInfo(block: (List<InteractStatus?>) -> Unit) {
        // 消费指针状态
        block(statusList)
        // 重置当前所有指针状态
        statusList.fastForEachIndexed { index, status ->
            when (status) {
                null -> { }
                is InteractStatus.Down -> statusList[index] = InteractStatus.AwaitUp(status.id)
                is InteractStatus.Up -> statusList[index] = null
                else -> { } // AwaitUp 不处理
            }
        }
    }

    override fun preUpdate(tick: Int) {
        var isDirty = false
        for (i in 1 ..< infos.size) {
            val info = infos[i]
            val cp = info.currentProgress
            val tp = info.targetProgress
            if (cp == tp) continue
            val step = tick / InteractInfo.BRUSH_DURATION
            info.currentProgress = if (cp < tp) (cp + step).fastCoerceAtMost(tp) else (cp - step).fastCoerceAtLeast(tp)
            isDirty = true
        }
        if (isDirty) updateDirty()
    }

    override fun PrepareDrawer.prePrepareDraw(viewportSize: Size, viewportBounds: Rect) {
        if (lastViewportSize != viewportSize) {
            lastViewportSize = viewportSize
            val (w, h) = viewportSize
            w1 = w / 3
            w2 = w * 2 / 3
            w3 = w
            if (w >= h) { // 横屏满屏
                h0 = 0f
                h1 = h / 3
                h2 = h * 2 / 3
            }
            else { // 竖屏半屏
                val half = h / 2
                h0 = half
                h1 = half * 4 / 3
                h2 = half * 5 / 3
            }
            h3 = h

            val ic = InteractInfo.INDICATOR_RADIUS
            val ivh = (h1 - h0) / 2
            val ivs = Size(ic * 2, ivh)
            val ihs = Size(ivh, ic * 2)
            infos[1].mRect = Rect(Offset(-ic, h0 + ivh / 2), ivs)
            infos[2].mRect = Rect(Offset(-ic, h1 + ivh / 2), ivs)
            infos[3].mRect = Rect(Offset(-ic, h2 + ivh / 2), ivs)
            infos[4].mRect = Rect(Offset((w1 * 3 - ivh) / 2, h3 - ic), ihs)
            infos[5].mRect = Rect(Offset(w3 - ic, h2 + ivh / 2), ivs)
            infos[6].mRect = Rect(Offset(w3 - ic, h1 + ivh / 2), ivs)
            infos[7].mRect = Rect(Offset(w3 - ic, h0 + ivh / 2), ivs)
        }
    }

    override fun Drawer.preOnDraw() {
        for (i in 1 ..< infos.size) {
            with(infos[i]) { drawInteract() }
        }
    }
}