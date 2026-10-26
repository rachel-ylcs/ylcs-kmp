package love.yinlin.nativecompose.ui

import androidx.compose.runtime.AbstractApplier

/** A composition without UI nodes must never request a tree mutation. */
internal class UnitApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = error("Unexpected UI node")
    override fun insertBottomUp(index: Int, instance: Unit) = error("Unexpected UI node")
    override fun move(from: Int, to: Int, count: Int) = error("Unexpected UI node")
    override fun remove(index: Int, count: Int) = error("Unexpected UI node")
    override fun onClear() {}
}
