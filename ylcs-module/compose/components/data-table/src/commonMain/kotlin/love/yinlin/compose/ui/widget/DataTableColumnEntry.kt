package love.yinlin.compose.ui.widget

import androidx.compose.runtime.*
import kotlin.uuid.Uuid

@Stable
internal class DataTableColumnEntry(val column: DataTableColumn, val sourceIndex: Int) {
    val key: String = Uuid.generateV7().toString()

    val title get() = column.title
    val width get() = column.width
    private var sortingCellCount by mutableIntStateOf(0)
    val canSort by derivedStateOf { sortingCellCount > 0 }

    fun addRow(item: DataTableItem) {
        if (item.cellOrNull(sourceIndex) is DataTableSortableCell) sortingCellCount++
    }

    fun addRows(items: Collection<DataTableItem>) {
        sortingCellCount += items.count { it.cellOrNull(sourceIndex) is DataTableSortableCell }
    }

    fun removeRow(item: DataTableItem) {
        if (item.cellOrNull(sourceIndex) is DataTableSortableCell) sortingCellCount--
    }

    fun clearRows() {
        sortingCellCount = 0
    }
}
