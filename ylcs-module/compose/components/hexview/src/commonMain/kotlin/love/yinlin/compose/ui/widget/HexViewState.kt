package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

@Stable
class HexViewState(private val data: ByteArray) {
    val size: Int get() = data.size

    internal var selectionAnchor by mutableIntStateOf(-1)
        private set

    var selectedIndex by mutableIntStateOf(-1)
        private set

    val selectedRange: IntRange get() = if (selectedIndex < 0) IntRange.EMPTY else minOf(selectionAnchor, selectedIndex) .. maxOf(selectionAnchor, selectedIndex)

    operator fun get(index: Int): Byte = data[index]

    fun select(index: Int, endIndex: Int = index) {
        require(index in data.indices && endIndex in data.indices) { "HexView selection is out of bounds" }
        selectionAnchor = index
        selectedIndex = endIndex
    }

    internal fun select(index: Int, extend: Boolean) {
        if (extend && selectedIndex == index) return
        select(if (extend && selectionAnchor >= 0) selectionAnchor else index, index)
    }

    fun clearSelection() {
        selectionAnchor = -1
        selectedIndex = -1
    }
}