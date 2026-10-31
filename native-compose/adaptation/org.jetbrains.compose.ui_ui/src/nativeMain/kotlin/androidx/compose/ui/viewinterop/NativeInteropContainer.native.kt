package androidx.compose.ui.viewinterop

import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.node.traverseDescendantsInDrawOrder
import androidx.compose.ui.window.NativeWindowContext

internal class NativeInteropContainer(override val root: NativeWindowContext, private val dispatchUpdate: (() -> Unit) -> Unit) : InteropContainer {
    private val holders = mutableMapOf<NativeInteropView, InteropViewHolder>()
    private val created: MutableSet<NativeInteropView> = []
    private var order: List<NativeInteropView> = []
    private var closed = false
    override var rootModifier: TrackInteropPlacementModifierNode? = null
    override val snapshotObserver = SnapshotStateObserver { update ->
        dispatchUpdate { if (!closed) update() }
    }.apply { start() }

    override fun contains(holder: InteropViewHolder): Boolean = holders[holder.interopView] === holder
    override fun holderOfView(view: InteropView): InteropViewHolder? = holders[view]
    fun containsView(view: NativeInteropView): Boolean = holders.containsKey(view)

    fun register(view: NativeInteropView) {
        check(!closed) { "The Native interop container has been closed" }
        created.add(view)
    }

    fun unregister(view: NativeInteropView) {
        created.remove(view)
        holders.remove(view)?.removeInteropView(root)
        order = order.filterNot { it === view }
    }

    fun hasNativeKeyboardFocus(): Boolean {
        var focused = false
        for (view in holders.keys) if (view.hasFocus) focused = true
        return focused
    }

    override fun place(holder: InteropViewHolder) {
        if (closed) return
        val view = holder.interopView as NativeInteropView
        if (holders.put(view, holder) == null) holder.insertInteropView(root, countInteropComponentsBelow(holder))
        updateOrder()
    }

    override fun unplace(holder: InteropViewHolder) {
        val view = holder.interopView as NativeInteropView
        if (holders.remove(view) != null) {
            try { view.hide() }
            finally {
                holder.removeInteropView(root)
                order = order.filterNot { it === view }
            }
        }
    }

    fun updateOrder() {
        if (holders.isEmpty()) return
        val ordered: MutableList<NativeInteropView> = []
        val remaining = holders.values.toMutableSet()
        rootModifier?.traverseDescendantsInDrawOrder { node ->
            val holder = node.interopViewHolder
            if (holder != null && remaining.remove(holder)) ordered.add(holder.interopView as NativeInteropView)
            true
        }
        for (holder in remaining) ordered.add(holder.interopView as NativeInteropView)
        if (ordered != order) {
            for (view in ordered) view.bringToFront()
            order = ordered
        }
    }

    override fun scheduleUpdate(action: () -> Unit) {
        if (!closed) action()
    }

    fun dispose() {
        if (closed) return
        closed = true
        snapshotObserver.stop()
        snapshotObserver.clear()
        val remaining = created.toList()
        created.clear()
        holders.clear()
        order = []
        rootModifier = null
        var failure: Throwable? = null
        for (view in remaining) {
            try { view.close() }
            catch (error: Throwable) {
                if (failure == null) failure = error
                else if (failure !== error) failure.addSuppressed(error)
            }
        }
        failure?.let { throw it }
    }
}
