package androidx.compose.ui.viewinterop

import androidx.compose.runtime.CompositeKeyHashCode
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.boundsInWindow

internal class NativeInteropViewHolder<T : NativeInteropView>(
    factory: () -> T,
    container: NativeInteropContainer,
    compositeKeyHashCode: CompositeKeyHashCode
) : TypedInteropViewHolder<T>(factory, container, Any(), compositeKeyHashCode) {
    override val measurePolicy = MeasurePolicy { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) { }
    }

    init {
        platformModifier = Modifier.pointerInteropFilter(this).drawBehind {
            drawRect(Color.Transparent, blendMode = BlendMode.Clear)
        }
    }

    override fun layoutAccordingTo(layoutCoordinates: LayoutCoordinates) {
        val bounds = layoutCoordinates.boundsInWindow(clipBounds = false)
        val visible = layoutCoordinates.boundsInWindow().intersect(bounds)
        if (!bounds.isFinite || !visible.isFinite || bounds.isEmpty || visible.isEmpty) interopView.hide()
        else interopView.place(bounds, visible)
    }

    override fun changeInteropViewIndex(root: InteropViewGroup, index: Int) = interopView.bringToFront()
    override fun dispatchToView(pointerEvent: PointerEvent) {
        pointerEvent.changes.forEach { it.consume() }
    }
}
