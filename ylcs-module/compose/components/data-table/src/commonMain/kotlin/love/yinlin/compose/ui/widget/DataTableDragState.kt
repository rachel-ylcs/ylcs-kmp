package love.yinlin.compose.ui.widget

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import kotlin.math.abs

@Stable
internal class DataTableDragState {
    var columnKey by mutableStateOf<String?>(null)
    var rowId by mutableStateOf<Long?>(null)
    var targetIndex by mutableIntStateOf(-1)
    var columnOffset by mutableFloatStateOf(0f)
    var columnPointer by mutableFloatStateOf(0f)
    var rowPointer by mutableFloatStateOf(0f)
    var rowGrabOffset = 0f
    var rowOriginIndex = -1

    fun clear() {
        columnKey = null
        rowId = null
        targetIndex = -1
        columnOffset = 0f
    }

    fun updateColumnTarget(columns: List<DataTableColumnEntry>, widths: DataTableWidths, density: Float) {
        val key = columnKey ?: return
        var left = 0f
        var center = 0f
        for (column in columns) {
            val width = widths[column.key].value * density
            if (column.key == key) center = left + width / 2 + columnOffset
            left += width
        }
        left = 0f
        targetIndex = columns.lastIndex
        for ([index, column] in columns.withIndex()) {
            left += widths[column.key].value * density
            if (center < left) { targetIndex = index; break }
        }
    }

    fun updateRowTarget(list: LazyListState, rowCount: Int) {
        var distance = Float.MAX_VALUE
        for (item in list.layoutInfo.visibleItemsInfo) {
            if (item.key !is Long || item.index >= rowCount) continue
            val current = abs(rowPointer - item.offset - item.size / 2f)
            if (current < distance) {
                distance = current
                targetIndex = item.index
            }
        }
        if (distance == Float.MAX_VALUE && rowCount > 0) targetIndex = rowCount - 1
    }
}
