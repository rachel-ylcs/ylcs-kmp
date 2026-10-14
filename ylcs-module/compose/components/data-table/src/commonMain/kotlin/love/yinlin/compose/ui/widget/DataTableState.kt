package love.yinlin.compose.ui.widget

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.toMutableStateList
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.reflect.metaRawSimpleClassName
import love.yinlin.reflect.metaSimpleClassName
import kotlin.reflect.KClass

@Stable
class DataTableState<T : DataTableItem>(columns: List<DataTableColumn>, items: List<T> = []) {
    @Stable
    private inner class ItemList : AbstractMutableList<T>() {
        override val size: Int get() = rowList.size
        override fun get(index: Int): T = rowList[index]
        override fun add(index: Int, element: T) { addAll(index, [element]) }
        override fun addAll(index: Int, elements: Collection<T>): Boolean {
            checkInsertIndex(index, size)
            val added = elements.toList()
            if (added.isEmpty()) return false
            commitEdit()
            Snapshot.withMutableSnapshot {
                val ids = added.map { nextId++ }
                rowList.addAll(index, added)
                rowIds.addAll(index, ids)
                for (offset in added.indices) rowMap[ids[offset]] = added[offset]
                for (column in columnList) column.addRows(added)
                rowsChanged()
                modCount++
            }
            return true
        }

        override fun addAll(elements: Collection<T>): Boolean = addAll(size, elements)

        override fun set(index: Int, element: T): T {
            val previous = rowList[index]
            if (previous === element) return previous
            commitEdit()
            Snapshot.withMutableSnapshot {
                removedRow(rowIds[index])
                val id = nextId++
                rowList.removeAt(index)
                rowList.add(index, element)
                rowIds[index] = id
                rowMap[id] = element
                for (column in columnList) column.addRow(element)
                rowsChanged()
            }
            return previous
        }

        override fun removeAt(index: Int): T {
            val previous = rowList[index]
            commitEdit()
            Snapshot.withMutableSnapshot {
                rowList.removeAt(index)
                removedRow(rowIds.removeAt(index))
                rowsChanged()
                modCount++
            }
            return previous
        }

        override fun clear() {
            if (isEmpty()) return
            commitEdit()
            Snapshot.withMutableSnapshot {
                rowList.clear()
                rowIds.clear()
                rowMap.clear()
                for (column in columnList) column.clearRows()
                selectedAddress = null
                clearError()
                rowsChanged()
                modCount++
            }
        }

        fun moved() { modCount++ }
    }

    @Stable
    private class SortRow<T>(val item: T, val id: Long, val cell: DataTableSortableCell?, val group: Int)

    internal val columnList: List<DataTableColumnEntry> field = columns.mapIndexed { index, column ->
        DataTableColumnEntry(column, index)
    }.toMutableStateList()

    private val columnMap = columnList.associateByTo(mutableMapOf()) { it.key }
    private val rowList = mutableStateListOf<T>()
    private val rowIds = mutableStateListOf<Long>()
    private val rowMap = mutableMapOf<Long, T>()
    private var sortedView: Pair<List<T>, List<Long>>? by mutableRefStateOf(null)
    private var nextId = 0L
    private val mutableItems = ItemList()
    private val currentRows: List<T> get() = sortedView?.first ?: rowList
    val rows: List<T> get() = currentRows.toList()

    val columns: List<DataTableColumn> get() = columnList.map { it.column }
    val rowCount: Int get() = rowList.size
    val columnCount: Int get() = columnList.size
    val sortColumn: Int? get() = sortColumnKey?.let { key -> columnList.indexOfFirst { it.key == key }.takeIf { it >= 0 } }
    val selectedCell: DataTableCellPosition? get() = selectedAddress?.let { (val rowId, val columnKey) ->
        val row = (sortedView?.second ?: rowIds).indexOf(rowId)
        val column = columnList.indexOfFirst { it.key == columnKey }
        if (row < 0 || column < 0) null else DataTableCellPosition(row, column)
    }

    var editing by mutableStateOf<DataTableEditState?>(null)
        private set
    var sortDirection by mutableStateOf(DataTableSortDirection.None)
        private set
    var error by mutableStateOf(false)
        private set

    internal var selectedAddress by mutableStateOf<DataTableCellAddress?>(null)
        private set
    internal var sortColumnKey by mutableStateOf<String?>(null)
        private set
    internal var invalidCell by mutableStateOf<DataTableCellAddress?>(null)
        private set
    internal var structureVersion by mutableIntStateOf(0)
        private set
    internal var sortVersion by mutableIntStateOf(0)
        private set

    init {
        mutableItems.addAll(items)
    }

    fun add(item: T): Boolean = mutableItems.add(item)
    fun addAll(items: Collection<T>): Boolean = mutableItems.addAll(items)
    fun remove(item: T): Boolean = mutableItems.remove(item)
    fun clear() = mutableItems.clear()

    operator fun get(row: Int): T = currentRows[row]

    operator fun set(row: Int, item: T) {
        mutableItems[indexOfRow(rowIdAt(row))] = item
    }

    fun remove(row: Int): T = mutableItems.removeAt(indexOfRow(rowIdAt(row)))

    @PublishedApi
    internal fun cellOf(row: Int, column: Int): DataTableCell {
        val item = this[row]
        val entry = columnList[column]
        val value = item.cellOrNull(entry.sourceIndex) ?: throw IndexOutOfBoundsException("Missing DataTable cell ($row, $column) at item index " + entry.sourceIndex)
        val expected = entry.column.cellType
        if (expected != null && !expected.isInstance(value)) throw ClassCastException("DataTable cell ($row, $column) does not match column type " + expected.metaRawSimpleClassName)
        return value
    }

    inline fun <reified C : DataTableCell> cell(row: Int, column: Int): C = cellOf(row, column) as? C ?: throw ClassCastException("DataTable cell ($row, $column) is not " + metaSimpleClassName<C>())

    internal fun rowIdAt(index: Int): Long = sortedView?.second?.get(index) ?: rowIds[index]
    internal fun rowSnapshot(): Pair<List<T>, List<Long>> = sortedView ?: (rowList.toList() to rowIds.toList())
    internal fun cellOrNull(item: DataTableItem, columnKey: String): DataTableCell? = columnMap[columnKey]?.let { item.cellOrNull(it.sourceIndex) }
    internal fun findRow(rowId: Long): T? = rowMap[rowId]
    internal fun indexOfRow(rowId: Long): Int = rowIds.indexOf(rowId)

    fun addColumn(column: DataTableColumn, index: Int = columnList.size) {
        checkInsertIndex(index, columnList.size)
        val used: MutableSet<Int> = columnList.mapTo([]) { it.sourceIndex }
        var sourceIndex = 0
        while (sourceIndex in used) sourceIndex++
        val entry = DataTableColumnEntry(column, sourceIndex)
        clearSelection()
        Snapshot.withMutableSnapshot {
            entry.addRows(rowList)
            columnList.add(index, entry)
            columnMap[entry.key] = entry
            structureVersion++
        }
    }

    fun removeColumn(column: Int): DataTableColumn {
        val entry = columnList[column]
        clearSelection()
        Snapshot.withMutableSnapshot {
            columnList.removeAt(column)
            columnMap.remove(entry.key)
            if (invalidCell?.columnKey == entry.key) clearError()
            if (sortColumnKey == entry.key) resetSort()
            structureVersion++
        }
        return entry.column
    }

    fun moveColumn(from: Int, to: Int) {
        val entry = columnList[from]
        checkElementIndex(to, columnList.size)
        moveColumnById(entry.key, to)
    }

    internal fun moveColumnById(columnKey: String, toIndex: Int) {
        val fromIndex = columnList.indexOfFirst { it.key == columnKey }
        if (fromIndex < 0 || toIndex !in columnList.indices || fromIndex == toIndex) return
        clearSelection()
        Snapshot.withMutableSnapshot {
            columnList.add(toIndex, columnList.removeAt(fromIndex))
            structureVersion++
        }
    }

    fun moveRow(from: Int, to: Int) {
        val rowId = rowIdAt(from)
        checkElementIndex(to, rowList.size)
        moveRowById(rowId, to)
    }

    internal fun moveRowById(rowId: Long, toIndex: Int) {
        if (sortDirection != DataTableSortDirection.None) return
        val fromIndex = rowIds.indexOf(rowId)
        if (fromIndex < 0 || toIndex !in rowList.indices || fromIndex == toIndex) return
        clearSelection()
        Snapshot.withMutableSnapshot {
            rowList.add(toIndex, rowList.removeAt(fromIndex))
            rowIds.add(toIndex, rowIds.removeAt(fromIndex))
            mutableItems.moved()
            structureVersion++
        }
    }

    fun toggleSort(column: Int) = sortById(columnList[column].key)

    internal fun sortById(columnKey: String) {
        if (columnMap[columnKey]?.canSort != true) return
        commitEdit()
        Snapshot.withMutableSnapshot {
            sortDirection = when {
                sortColumnKey != columnKey -> DataTableSortDirection.Ascending
                sortDirection == DataTableSortDirection.Ascending -> DataTableSortDirection.Descending
                sortDirection == DataTableSortDirection.Descending -> DataTableSortDirection.None
                else -> DataTableSortDirection.Ascending
            }
            sortColumnKey = if (sortDirection == DataTableSortDirection.None) null else columnKey
            applySort()
            sortVersion++
        }
    }

    fun clearSort() {
        commitEdit()
        resetSort()
    }

    private fun resetSort() {
        sortColumnKey = null
        sortDirection = DataTableSortDirection.None
        sortedView = null
        sortVersion++
    }

    private fun applySort() {
        val key = sortColumnKey
        if (key == null) {
            sortedView = null
            return
        }
        if (columnMap[key]?.canSort != true) {
            resetSort()
            return
        }
        val groups = mutableMapOf<KClass<out DataTableSortableCell>, Int>()
        val entries = rowList.mapIndexed { index, item ->
            val cell = cellOrNull(item, key) as? DataTableSortableCell
            val group = cell?.sortType?.let { groups.getOrPut(it) { groups.size } } ?: Int.MAX_VALUE
            SortRow(item, rowIds[index], cell, group)
        }
        val descending = sortDirection == DataTableSortDirection.Descending
        val sorted = entries.sortedWith { left, right ->
            when {
                left.group != right.group -> left.group.compareTo(right.group)
                left.cell == null || right.cell == null -> 0
                descending -> right.cell.compareTo(left.cell)
                else -> left.cell.compareTo(right.cell)
            }
        }
        sortedView = sorted.map { it.item } to sorted.map { it.id }
    }

    fun editCell(row: Int, column: Int) {
        cellOf(row, column)
        editCellById(rowIdAt(row), columnList[column].key)
    }

    internal fun editCellById(rowId: Long, columnKey: String) {
        val item = rowMap[rowId] ?: return
        val type = cellOrNull(item, columnKey) ?: return
        val position = DataTableCellAddress(rowId, columnKey)
        if (editing?.cell == position) return
        commitEdit()
        clearError()
        selectedAddress = position
        if (type.toggle()) {
            if (sortColumnKey == columnKey) applySort()
        }
        else if (type.editable) editing = DataTableEditState(this, position, type)
    }

    fun commitEdit(): Boolean = editing?.let { finishEdit(it) } ?: true

    fun clearSelection(): Boolean {
        val valid = commitEdit()
        selectedAddress = null
        return valid
    }

    internal fun publishEdit(edit: DataTableEditState, value: String): Boolean {
        if (editing !== edit || edit.cell.rowId !in rowMap) return false
        if (!edit.type.update(value)) {
            invalidCell = edit.cell
            error = true
            return false
        }
        clearError()
        return true
    }

    internal fun finishEdit(edit: DataTableEditState): Boolean {
        if (editing !== edit) return true
        val valid = edit.onInput(edit.text)
        editing = null
        selectedAddress = null
        if (valid) clearError()
        else {
            invalidCell = edit.cell
            error = true
        }
        if (sortColumnKey == edit.cell.columnKey) applySort()
        return valid
    }

    internal fun cancelEdit(edit: DataTableEditState) {
        if (editing === edit) {
            editing = null
            selectedAddress = null
            edit.restoreValue()
            clearError()
            if (sortColumnKey == edit.cell.columnKey) applySort()
        }
    }

    internal fun clearError() {
        error = false
        invalidCell = null
    }

    private fun removedRow(rowId: Long) {
        val item = rowMap.remove(rowId)
        if (item != null) for (column in columnList) column.removeRow(item)
        if (selectedAddress?.rowId == rowId) selectedAddress = null
        if (invalidCell?.rowId == rowId) clearError()
    }

    private fun checkElementIndex(index: Int, size: Int) {
        if (index !in 0 ..< size) throw IndexOutOfBoundsException("DataTable index $index exceeds size $size")
    }

    private fun checkInsertIndex(index: Int, size: Int) {
        if (index !in 0 .. size) throw IndexOutOfBoundsException("DataTable insertion index $index exceeds size $size")
    }

    private fun rowsChanged() {
        structureVersion++
        applySort()
    }
}
