package love.yinlin.compose.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.floating.Flyout
import love.yinlin.compose.ui.floating.FlyoutPosition
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleEllipsisText
import kotlin.math.abs
import kotlin.math.roundToInt

private val DataTableNumberInput = Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]*)(?:[eE][+-]?[0-9]*)?|[+-]?")

/**
 * 单元格的值以字符串保存，便于读取、导出和持久化；只有 [normalize] 接受的值才能写入表格。
 * 可实现此接口扩展值的校验、排序、显示和内嵌编辑器，无需给 [DataTable] 增加回调。
 * [Editor] 的自定义实现应通过 [DataTableEditState] 修改草稿、提交或取消。
 */
@Stable
interface DataTableCellType {
    val defaultValue: String
    val keyboardType: KeyboardType get() = KeyboardType.Text
    val errorMessage: String get() = "单元格内容无效"

    /** 返回规范化的值；null 表示不允许保存。 */
    fun normalize(value: String): String?

    /** 输入期间允许的中间状态；只有通过 [normalize] 的内容才会即时保存。 */
    fun acceptsInput(value: String): Boolean = true

    fun compare(left: String, right: String): Int = left.compareTo(right, ignoreCase = true)

    /** 每次排序创建一个比较器，昂贵的值解析可缓存在本次排序中。 */
    fun comparator(): Comparator<String> = Comparator { left, right -> compare(left, right) }

    /** 非 null 时单击直接写入此值，例如 bool 的切换。 */
    fun toggle(value: String): String? = null

    @Composable
    fun Display(value: String, modifier: Modifier) {
        SimpleEllipsisText(value, modifier = modifier.padding(horizontal = Theme.padding.h), style = Theme.typography.v8)
    }

    @Composable
    fun Editor(state: DataTableEditState, modifier: Modifier) {
        DataTableTextEditor(state, modifier)
    }

    object Text : DataTableCellType {
        override val defaultValue: String = ""
        override fun normalize(value: String): String = value
    }

    object Bool : DataTableCellType {
        override val defaultValue: String = "false"
        override val errorMessage: String = "请输入 true 或 false"
        override fun normalize(value: String): String? = value.trim().lowercase().takeIf { it == "true" || it == "false" }
        override fun compare(left: String, right: String): Int = left.toBoolean().compareTo(right.toBoolean())
        override fun toggle(value: String): String = (!value.toBoolean()).toString()

        @Composable
        override fun Display(value: String, modifier: Modifier) {
            val checked = value.toBoolean()
            Row(
                modifier = modifier.padding(horizontal = Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h / 2)
            ) {
                Icon(
                    icon = if (checked) Icons.CheckBox else Icons.CheckBoxOutlineBlank,
                    color = if (checked) Theme.color.primary else Theme.color.onSurfaceVariant,
                    modifier = Modifier.size(Theme.size.smallIcon)
                )
                SimpleEllipsisText(value, style = Theme.typography.v8)
            }
        }
    }

    /** 保留原始数字文本，使用有限 Double 校验和比较，支持小数、负数及科学计数法。 */
    object Number : DataTableCellType {
        override val defaultValue: String = "0"
        override val keyboardType: KeyboardType = KeyboardType.Decimal
        override val errorMessage: String = "请输入有效数字"
        override fun acceptsInput(value: String): Boolean = DataTableNumberInput.matches(value)
        override fun normalize(value: String): String? = value.trim().takeIf {
            it.isNotEmpty() && acceptsInput(it) && it.toDoubleOrNull()?.isFinite() == true
        }
        override fun compare(left: String, right: String): Int = left.toDouble().compareTo(right.toDouble())
        override fun comparator(): Comparator<String> {
            val cache = mutableMapOf<String, Double>()
            return Comparator { left, right -> cache.getOrPut(left) { left.toDouble() }.compareTo(cache.getOrPut(right) { right.toDouble() }) }
        }
    }

    object DateTime : DataTableCellType {
        override val defaultValue: String = "1970-01-01T00:00"
        override val errorMessage: String = "请输入日期时间，例如 2026-10-13T09:30"
        override fun normalize(value: String): String? = runCatching { LocalDateTime.parse(value.trim()).toString() }.getOrNull()
        override fun compare(left: String, right: String): Int = LocalDateTime.parse(left).compareTo(LocalDateTime.parse(right))
        override fun comparator(): Comparator<String> {
            val cache = mutableMapOf<String, LocalDateTime>()
            return Comparator { left, right -> cache.getOrPut(left) { LocalDateTime.parse(left) }.compareTo(cache.getOrPut(right) { LocalDateTime.parse(right) }) }
        }
    }

    /** 选项顺序同时用于排序；选项必须非空且不重复。 */
    class Selection(options: List<String>) : DataTableCellType {
        val options: List<String> = options.toList()

        init {
            require(this.options.isNotEmpty() && this.options.distinct().size == this.options.size) { "DataTable selection options must be non-empty and unique" }
        }

        private val optionOrder = this.options.withIndex().associate { [index, option] -> option to index }

        override val defaultValue: String get() = options.first()
        override val errorMessage: String = "请选择列表中的值"
        override fun normalize(value: String): String? = value.takeIf { it in optionOrder }
        override fun compare(left: String, right: String): Int = optionOrder.getValue(left).compareTo(optionOrder.getValue(right))

        @Composable
        override fun Display(value: String, modifier: Modifier) {
            Row(modifier.padding(horizontal = Theme.padding.h), verticalAlignment = Alignment.CenterVertically) {
                SimpleEllipsisText(value, modifier = Modifier.weight(1f), style = Theme.typography.v8)
                Icon(Icons.KeyboardArrowDown, color = Theme.color.onSurfaceVariant, modifier = Modifier.size(Theme.size.smallIcon))
            }
        }

        @Composable
        override fun Editor(state: DataTableEditState, modifier: Modifier) {
            DataTableSelectionEditor(state, this, modifier)
        }
    }
}

/** [key] 是数据关联的稳定键，调整列顺序不会改变它。标题只读，未指定 [width] 时使用主题尺寸。 */
@Stable
class DataTableColumn(
    val key: String,
    val title: String,
    val type: DataTableCellType = DataTableCellType.Text,
    val width: Dp = Dp.Unspecified,
    defaultValue: String = type.defaultValue,
) {
    val defaultValue: String = requireNotNull(type.normalize(defaultValue)) { "Invalid default value for DataTable column $key" }

    init {
        require(key.isNotEmpty() && title.isNotBlank()) { "DataTable columns need a key and title" }
        require(width == Dp.Unspecified || (width.value.isFinite() && width > 0.dp)) { "DataTable column width must be positive" }
    }
}

@Stable
class DataTableRow internal constructor(val id: Long, values: Map<String, String>) {
    private val cells = mutableStateMapOf<String, String>().apply { putAll(values) }
    val values: Map<String, String> field = cells

    operator fun get(columnKey: String): String = cells.getValue(columnKey)

    internal fun update(columnKey: String, value: String) { cells[columnKey] = value }
}

@Immutable
data class DataTableCell(val rowId: Long, val columnKey: String)

@Stable
enum class DataTableSortDirection { None, Ascending, Descending }

/** 自定义内嵌编辑器使用的草稿状态；合法输入即时保存，[commit] 结束编辑，[cancel] 恢复进入编辑前的值，两者均取消选中。 */
@Stable
class DataTableEditState internal constructor(
    private val table: DataTableState,
    val cell: DataTableCell,
    val type: DataTableCellType,
    internal val initialValue: String,
) {
    internal val rawState = TextFieldState(initialValue, TextRange(0, initialValue.length))
    internal val isActive: Boolean get() = table.editing === this
    private var lastInput = initialValue
    val text: String get() = rawState.text.toString()

    val error: String? get() = if (type.normalize(text) == null) type.errorMessage else null

    fun updateText(value: String): Boolean {
        if (!isActive || !type.acceptsInput(value)) return false
        rawState.setTextAndPlaceCursorAtEnd(value)
        onInput(value)
        return true
    }

    fun commit(): Boolean = table.finishEdit(this)
    fun cancel() = table.cancelEdit(this)
    internal fun onInput(value: String) {
        if (!isActive || lastInput == value) return
        lastInput = value
        table.publishEdit(this, value)
    }
}

/**
 * 表格的数据和交互状态。输入列描述会复制到此状态，列数固定，数据始终按列 key 关联。
 * [rows] 是当前显示顺序，[originalRows] 保留未排序的行顺序，排序只生成新列表，不重排原始数据。
 * 两个列表按相同的行 id 关联，编辑、增加和删除在原始数据中生效；[snapshot] 导出当前显示顺序。
 * 编辑草稿存放在 [editing]，合法输入即时写入数据，无效草稿保留最近的合法值并给出 [validationError]。
 * 提交或取消当前编辑时同时取消选中；失焦或结构操作结束编辑，编辑排序列期间保持行位置，结束后统一更新排序列表。
 * 仅未排序时可手动调整原始行顺序；排序期间仍可调整列顺序，增删行时会更新排序列表。
 */
@Stable
class DataTableState(columns: List<DataTableColumn>, rows: List<Map<String, String>> = emptyList()) {
    private val columnList = mutableStateListOf<DataTableColumn>()
    private val rowList = mutableStateListOf<DataTableRow>()
    private var sortedRows by mutableStateOf<List<DataTableRow>?>(null)
    private val columnMap: Map<String, DataTableColumn>
    private val rowMap = mutableMapOf<Long, DataTableRow>()
    private var nextId = 0L

    val columns: List<DataTableColumn> field = columnList
    val originalRows: List<DataTableRow> field = rowList
    val rows: List<DataTableRow> get() = sortedRows ?: rowList
    val rowCount: Int get() = rowList.size

    var selectedCell by mutableStateOf<DataTableCell?>(null)
        private set
    var editing by mutableStateOf<DataTableEditState?>(null)
        private set
    var sortColumnKey by mutableStateOf<String?>(null)
        private set
    var sortDirection by mutableStateOf(DataTableSortDirection.None)
        private set
    var validationError by mutableStateOf<String?>(null)
        private set
    internal var invalidCell by mutableStateOf<DataTableCell?>(null)
        private set

    init {
        require(columns.isNotEmpty() && columns.map { it.key }.distinct().size == columns.size) { "DataTable columns must be non-empty with unique keys" }
        columnList.addAll(columns.map { DataTableColumn(it.key, it.title, it.type, it.width, it.defaultValue) })
        columnMap = columnList.associateBy { it.key }
        for (values in rows) insertRow(normalizeRow(values))
    }

    private fun normalizeRow(values: Map<String, String>): Map<String, String> {
        require(values.keys.all { it in columnMap }) { "Unknown DataTable column key" }
        return columnList.associate { column ->
            column.key to requireNotNull(column.type.normalize(values[column.key] ?: column.defaultValue)) { "Invalid value for DataTable column ${column.key}" }
        }
    }

    private fun insertRow(values: Map<String, String>): DataTableRow {
        val row = DataTableRow(nextId++, values)
        rowMap[row.id] = row
        rowList.add(row)
        return row
    }

    /** 缺少的列使用列默认值。 */
    fun addRow(values: Map<String, String> = emptyMap()): DataTableRow {
        val normalized = normalizeRow(values)
        commitEdit()
        val row = insertRow(normalized)
        applySort()
        return row
    }

    /** 批量追加时只在最后排序一次，先验证整个批次以避免部分写入。 */
    fun addRows(values: List<Map<String, String>>): List<DataTableRow> {
        val normalized = values.map { normalizeRow(it) }
        commitEdit()
        return Snapshot.withMutableSnapshot {
            val added = normalized.map { insertRow(it) }
            applySort()
            added
        }
    }

    fun removeRow(rowId: Long) {
        val row = rowMap[rowId] ?: return
        commitEdit()
        Snapshot.withMutableSnapshot {
            rowList.remove(row)
            rowMap.remove(rowId)
            if (selectedCell?.rowId == rowId) selectedCell = null
            if (invalidCell?.rowId == rowId) clearError()
            applySort()
        }
    }

    fun updateCell(rowId: Long, columnKey: String, value: String): Boolean {
        val row = rowMap[rowId] ?: return false
        val column = columnMap[columnKey] ?: return false
        val normalized = column.type.normalize(value)
        if (normalized == null) {
            invalidCell = DataTableCell(rowId, columnKey)
            validationError = "${column.title}：${column.type.errorMessage}，原值已保留"
            return false
        }
        if (editing?.cell == DataTableCell(rowId, columnKey)) editing = null
        if (row[columnKey] != normalized) row.update(columnKey, normalized)
        clearError()
        if (sortColumnKey == columnKey) applySort()
        return true
    }

    fun moveColumn(columnKey: String, toIndex: Int) {
        require(toIndex in columnList.indices) { "DataTable column index is out of bounds" }
        val fromIndex = columnList.indexOfFirst { it.key == columnKey }
        if (fromIndex < 0 || fromIndex == toIndex) return
        commitEdit()
        Snapshot.withMutableSnapshot { columnList.add(toIndex, columnList.removeAt(fromIndex)) }
    }

    /** [toIndex] 是未排序列表的位置；排序期间不执行行顺序调整。 */
    fun moveRow(rowId: Long, toIndex: Int) {
        if (sortDirection != DataTableSortDirection.None) return
        require(toIndex in rowList.indices) { "DataTable row index is out of bounds" }
        val row = rowMap[rowId] ?: return
        commitEdit()
        val fromIndex = rowList.indexOf(row)
        if (fromIndex == toIndex) return
        Snapshot.withMutableSnapshot { rowList.add(toIndex, rowList.removeAt(fromIndex)) }
    }

    /** 同列循环切换未排序、升序、降序；切换其他列从升序开始。 */
    fun sortBy(columnKey: String) {
        require(columnKey in columnMap) { "Unknown DataTable column key" }
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
        }
    }

    fun clearSort() {
        commitEdit()
        Snapshot.withMutableSnapshot {
            sortColumnKey = null
            sortDirection = DataTableSortDirection.None
            sortedRows = null
        }
    }

    private fun applySort() {
        val key = sortColumnKey
        if (key == null) {
            sortedRows = null
            return
        }
        val type = columnMap.getValue(key).type
        val comparator = type.comparator()
        val descending = sortDirection == DataTableSortDirection.Descending
        sortedRows = rowList.sortedWith { left, right ->
            if (descending) comparator.compare(right[key], left[key]) else comparator.compare(left[key], right[key])
        }
    }

    fun editCell(rowId: Long, columnKey: String) {
        val row = rowMap[rowId] ?: return
        val column = columnMap[columnKey] ?: return
        val cell = DataTableCell(rowId, columnKey)
        if (editing?.cell == cell) return
        commitEdit()
        selectedCell = cell
        val toggled = column.type.toggle(row[columnKey])
        if (toggled != null) updateCell(rowId, columnKey, toggled)
        else startEdit(cell, column.type, row[columnKey])
    }

    private fun startEdit(cell: DataTableCell, type: DataTableCellType, value: String) {
        if (editing?.cell == cell) return
        commitEdit()
        clearError()
        selectedCell = cell
        editing = DataTableEditState(this, cell, type, value)
    }

    /** 结束当前编辑、取消选中并更新排序；合法输入无需等待此操作即可读取。 */
    fun commitEdit(): Boolean = editing?.let { finishEdit(it) } ?: true

    /** 结束编辑并取消单元格选中；无效草稿的错误标记仍然保留。 */
    fun clearSelection(): Boolean {
        val valid = commitEdit()
        selectedCell = null
        return valid
    }

    internal fun publishEdit(edit: DataTableEditState, value: String): Boolean {
        if (editing !== edit) return false
        val row = rowMap[edit.cell.rowId] ?: return false
        val normalized = edit.type.normalize(value)
        if (normalized == null) {
            invalidCell = edit.cell
            validationError = "${edit.type.errorMessage}，原值已保留"
            return false
        }
        if (row[edit.cell.columnKey] != normalized) row.update(edit.cell.columnKey, normalized)
        clearError()
        return true
    }

    internal fun finishEdit(edit: DataTableEditState): Boolean {
        if (editing !== edit) return true
        editing = null
        selectedCell = null
        val normalized = edit.type.normalize(edit.text)
        if (normalized == null) {
            invalidCell = edit.cell
            validationError = "${edit.type.errorMessage}，原值已保留"
            if (sortColumnKey == edit.cell.columnKey) applySort()
            return false
        }
        return updateCell(edit.cell.rowId, edit.cell.columnKey, normalized)
    }

    internal fun cancelEdit(edit: DataTableEditState) {
        if (editing === edit) {
            editing = null
            selectedCell = null
            updateCell(edit.cell.rowId, edit.cell.columnKey, edit.initialValue)
        }
    }

    internal fun clearError() { validationError = null; invalidCell = null }

    internal fun findRow(rowId: Long): DataTableRow? = rowMap[rowId]

    /** 按当前行顺序和列顺序读取最新合法数据，不包含无效草稿。 */
    fun snapshot(): List<Map<String, String>> = rows.map { row -> columnList.associate { it.key to row[it.key] } }
}

/** 参数只作为初值，后续变化通过 [DataTableState] 操作。 */
@Composable
fun rememberDataTableState(columns: List<DataTableColumn>, rows: List<Map<String, String>> = emptyList()): DataTableState = remember { DataTableState(columns, rows) }

/** 通用文本编辑器，可供自定义类型复用。合法输入即时保存，Enter 或失焦保存并结束编辑，Escape 恢复编辑前的值，结束时均取消选中。 */
@Composable
fun DataTableTextEditor(state: DataTableEditState, modifier: Modifier = Modifier) {
    val focus = LocalFocusManager.current
    val requester = remember(state) { FocusRequester() }
    var hadFocus by remember(state) { mutableStateOf(false) }
    val transformation = remember(state) {
        InputTransformation {
            val value = asCharSequence().toString()
            if (!state.type.acceptsInput(value)) revertAllChanges()
            else if (changes.changeCount > 0) state.onInput(value)
        }
    }
    val message = state.error

    Box(modifier.padding(horizontal = Theme.padding.h), contentAlignment = Alignment.CenterStart) {
        BasicTextField(
            state = state.rawState,
            inputTransformation = transformation,
            modifier = Modifier.fillMaxWidth().focusRequester(requester).onFocusChanged {
                if (it.isFocused) hadFocus = true
                else if (hadFocus) state.commit()
            }.onPreviewKeyEvent {
                when {
                    it.type != KeyEventType.KeyDown -> false
                    it.key == Key.Escape -> { state.cancel(); focus.clearFocus(); true }
                    it.key == Key.Enter || it.key == Key.NumPadEnter -> { state.commit(); focus.clearFocus(); true }
                    else -> false
                }
            }.semantics { if (message != null) error(message) },
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = Theme.typography.v8.copy(color = Theme.color.onSurface),
            cursorBrush = SolidColor(Theme.color.primary),
            keyboardOptions = KeyboardOptions(keyboardType = state.type.keyboardType, imeAction = ImeAction.Done, autoCorrectEnabled = false),
            onKeyboardAction = remember(state, focus) { KeyboardActionHandler { state.commit(); focus.clearFocus() } },
        )
    }

    LaunchedEffect(state) { requester.requestFocus() }
}

@Composable
private fun DataTableSelectionEditor(state: DataTableEditState, type: DataTableCellType.Selection, modifier: Modifier) {
    val options = type.options
    val focus = LocalFocusManager.current
    val density = LocalDensity.current
    val requester = remember(state) { FocusRequester() }
    val listState = rememberLazyListState(options.indexOf(state.text).coerceAtLeast(0))
    var hadFocus by remember(state) { mutableStateOf(false) }
    var menuWidth by remember(state) { mutableStateOf(0.dp) }
    var expanded by remember(state) { mutableStateOf(true) }

    Flyout(
        visible = expanded && menuWidth > 0.dp,
        onClickOutside = { expanded = false; state.commit() },
        position = FlyoutPosition.Bottom,
        focusable = false,
        space = 0.dp,
        flyout = {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(menuWidth).heightIn(max = Theme.size.cell2).clip(Theme.shape.v7)
                    .background(Theme.color.surface).border(Theme.border.v7, Theme.color.outline, Theme.shape.v7)
            ) {
                itemsIndexed(options, key = { _, option -> option }) { _, option ->
                    SimpleEllipsisText(
                        option,
                        color = if (option == state.text) Theme.color.primary else Theme.color.onSurface,
                        style = Theme.typography.v8,
                        modifier = Modifier.fillMaxWidth().background(if (option == state.text) Theme.color.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { state.updateText(option); expanded = false; state.commit(); focus.clearFocus() }
                            .padding(Theme.padding.value10)
                    )
                }
            }
        }
    ) {
        Row(
            modifier = modifier.onSizeChanged { menuWidth = with(density) { it.width.toDp() } }
                .focusRequester(requester).onFocusChanged {
                    if (it.isFocused) hadFocus = true
                    else if (hadFocus) { expanded = false; state.commit() }
                }.onPreviewKeyEvent {
                    if (it.type != KeyEventType.KeyDown) false else when (it.key) {
                        Key.Escape -> { state.cancel(); focus.clearFocus(); true }
                        Key.Enter, Key.NumPadEnter -> { state.commit(); focus.clearFocus(); true }
                        Key.DirectionDown, Key.DirectionUp -> {
                            val delta = if (it.key == Key.DirectionDown) 1 else -1
                            state.updateText(options[(options.indexOf(state.text) + delta).coerceIn(options.indices)])
                            true
                        }
                        else -> false
                    }
                }.clickable { expanded = !expanded }.padding(horizontal = Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText(state.text, modifier = Modifier.weight(1f), style = Theme.typography.v8)
            Icon(Icons.KeyboardArrowDown, color = Theme.color.primary, modifier = Modifier.size(Theme.size.smallIcon))
        }
    }

    LaunchedEffect(state) { requester.requestFocus() }
    LaunchedEffect(state.text) { listState.scrollToItem(options.indexOf(state.text).coerceAtLeast(0)) }
}

/** 在编辑器外统一处理焦点和销毁，扩展编辑器通过 updateText 即时写入合法值。 */
@Composable
private fun DataTableEditor(state: DataTableEditState, modifier: Modifier) {
    val requester = remember(state) { FocusRequester() }
    var hadFocus by remember(state) { mutableStateOf(false) }
    Box(modifier.focusRequester(requester).onFocusChanged {
        if (it.hasFocus) hadFocus = true
        else if (hadFocus) state.commit()
    }.focusGroup()) {
        state.type.Editor(state, Modifier.fillMaxSize())
    }
    LaunchedEffect(state) {
        requester.requestFocus()
        // 撤销和重做绕过 InputTransformation，补充监听；已即时处理的输入不会重复校验。
        snapshotFlow { state.text }.collect { state.onInput(it) }
    }
    DisposableEffect(state) { onDispose { state.commit() } }
}

@Stable
private class DataTableDragState {
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

    fun updateColumnTarget(columns: List<DataTableColumn>, widths: DataTableWidths, density: Float) {
        val key = columnKey ?: return
        var left = 0f
        var center = 0f
        for (column in columns) {
            val width = widths.getValue(column.key).value * density
            if (column.key == key) center = left + width / 2 + columnOffset
            left += width
        }
        left = 0f
        targetIndex = columns.lastIndex
        for ([index, column] in columns.withIndex()) {
            left += widths.getValue(column.key).value * density
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

@Immutable
private class DataTableWidths(private val values: Map<String, Dp>) {
    val total: Dp = values.values.fold(0.dp) { sum, width -> sum + width }
    fun getValue(key: String): Dp = values.getValue(key)
}

private fun Modifier.dataTableRowGestures(
    state: DataTableState,
    list: LazyListState,
    drag: DataTableDragState,
    gutter: Float,
    onStart: () -> Unit,
): Modifier = pointerInput(state, list, gutter, state.sortDirection == DataTableSortDirection.None) {
    awaitEachGesture {
        // 整个序号单元格处理单击、拖动和长按，在 Initial 阶段消费以避免列表滚动抢占。
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (down.position.x !in 0f ..< gutter) return@awaitEachGesture
        val item = list.layoutInfo.visibleItemsInfo.firstOrNull { down.position.y >= it.offset && down.position.y < it.offset + it.size } ?: return@awaitEachGesture
        val rowId = item.key as? Long ?: return@awaitEachGesture
        down.consume()
        val slop = if (down.type == PointerType.Mouse) viewConfiguration.touchSlop / 144f else viewConfiguration.touchSlop
        var cancelled = false
        var clicked = false
        val start = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var movement: PointerInputChange? = null
            while (movement == null && !cancelled) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || change.isConsumed || event.changes.any { it.id != down.id && it.pressed }) {
                    cancelled = true
                    continue
                }
                change.consume()
                if (!change.pressed) {
                    cancelled = true
                    clicked = change.position.x in 0f ..< gutter &&
                        change.position.y >= item.offset && change.position.y < item.offset + item.size
                }
                else if ((change.position - down.position).getDistance() > slop) movement = change
            }
            movement
        }
        if (cancelled) {
            // 按压已被本层消费，序号的短按在这里结束编辑并取消选中。
            if (clicked) onStart()
            return@awaitEachGesture
        }
        onStart()
        if (start == null) {
            state.removeRow(rowId)
            // 删除后继续消费本次按压，直到抬起，避免影响顶替此位置的下一行。
            while (currentEvent.changes.any { it.id == down.id && it.pressed }) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.firstOrNull { it.id == down.id }?.consume()
            }
            return@awaitEachGesture
        }
        if (state.sortDirection != DataTableSortDirection.None) return@awaitEachGesture
        drag.rowId = rowId
        drag.rowGrabOffset = down.position.y - item.offset
        drag.rowOriginIndex = state.rows.indexOfFirst { it.id == rowId }
        drag.rowPointer = start.position.y
        drag.targetIndex = item.index
        try {
            var dropped = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed || state.sortDirection != DataTableSortDirection.None) break
                change.consume()
                drag.rowPointer = change.position.y
                drag.updateRowTarget(list, state.rowCount)
                if (!change.pressed) { dropped = true; break }
            }
            if (dropped && drag.targetIndex in state.rows.indices) state.moveRow(rowId, drag.targetIndex)
        }
        finally { drag.clear() }
    }
}

@Composable
private fun DataTableCellFrame(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    invalid: Boolean = false,
    background: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    val outline = Theme.color.outline.copy(alpha = 0.35f)
    val highlight = if (invalid) Theme.color.error else Theme.color.primary
    val selectedBackground = Theme.color.primary.copy(alpha = 0.12f)
    val line = Theme.border.v8
    val selectionLine = Theme.border.v6

    Box(
        modifier = modifier.width(width).height(height).background(background)
            .background(if (isSelected) selectedBackground else Color.Transparent)
            .semantics { selected = isSelected }
            .drawWithContent {
                drawContent()
                val stroke = line.toPx()
                drawLine(outline, Offset(size.width - stroke / 2, 0f), Offset(size.width - stroke / 2, size.height), stroke)
                drawLine(outline, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
                if (isSelected || invalid) {
                    val border = selectionLine.toPx()
                    drawRect(highlight, topLeft = Offset(border / 2, border / 2), size = size.copy(width = (size.width - border).coerceAtLeast(0f), height = (size.height - border).coerceAtLeast(0f)), style = Stroke(border))
                }
            },
        contentAlignment = Alignment.CenterStart
    ) { content() }
}

@Composable
private fun DataTableHeaderCell(
    state: DataTableState,
    column: DataTableColumn,
    widths: DataTableWidths,
    height: Dp,
    drag: DataTableDragState,
    readonly: Boolean,
) {
    val density = LocalDensity.current
    val focus = LocalFocusManager.current
    val dragging = drag.columnKey == column.key
    val target = drag.columnKey != null && state.columns.getOrNull(drag.targetIndex)?.key == column.key

    DataTableCellFrame(
        width = widths.getValue(column.key),
        height = height,
        isSelected = target || dragging,
        background = Theme.color.backgroundVariant,
        modifier = Modifier.offset { IntOffset(if (dragging) drag.columnOffset.roundToInt() else 0, 0) }.zIndex(if (dragging) 2f else 0f)
            .clickable { state.clearSelection(); focus.clearFocus() }
            .then(if (readonly) Modifier else Modifier.pointerInput(state, column.key, widths, density) {
                detectDragGestures(
                    onDragStart = { pointer ->
                        state.clearSelection()
                        focus.clearFocus()
                        drag.clear()
                        drag.columnKey = column.key
                        val left = state.columns.takeWhile { it.key != column.key }.sumOf { widths.getValue(it.key).value.toDouble() }.toFloat()
                        drag.columnPointer = with(density) { left.dp.toPx() } + pointer.x
                        drag.updateColumnTarget(state.columns, widths, density.density)
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        drag.columnOffset += delta.x
                        drag.columnPointer += delta.x
                        drag.updateColumnTarget(state.columns, widths, density.density)
                    },
                    onDragEnd = {
                        if (drag.targetIndex in state.columns.indices) state.moveColumn(column.key, drag.targetIndex)
                        drag.clear()
                    },
                    onDragCancel = { drag.clear() },
                )
            }),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            SimpleEllipsisText(column.title, modifier = Modifier.weight(1f).padding(horizontal = Theme.padding.h), style = Theme.typography.v8.bold)
            val sorted = state.sortColumnKey == column.key
            Icon(
                icon = when {
                    !sorted -> Icons.SwapVert
                    state.sortDirection == DataTableSortDirection.Ascending -> Icons.ArrowUpward
                    else -> Icons.ArrowDownward
                },
                color = if (sorted) Theme.color.primary else Theme.color.onSurfaceVariant,
                tip = when {
                    !sorted -> "按此列升序排序"
                    state.sortDirection == DataTableSortDirection.Ascending -> "切换为降序"
                    else -> "恢复原始顺序"
                },
                modifier = Modifier.size(Theme.size.icon + Theme.padding.h).padding(Theme.padding.h / 2),
                onClick = { state.clearSelection(); focus.clearFocus(); state.sortBy(column.key) }
            )
        }
    }
}

@Composable
private fun DataTableBodyCell(state: DataTableState, row: DataTableRow, column: DataTableColumn, width: Dp, height: Dp, readonly: Boolean, preview: Boolean) {
    val cell = remember(row.id, column.key) { DataTableCell(row.id, column.key) }
    val selected by remember(state, cell) { derivedStateOf { state.selectedCell == cell } }
    val editing by remember(state, cell) { derivedStateOf { state.editing?.takeIf { it.cell == cell } } }
    val edit = if (preview || readonly) null else editing

    DataTableCellFrame(
        width = width,
        height = height,
        isSelected = selected && !preview,
        invalid = !preview && (edit?.error != null || state.invalidCell == cell),
        modifier = if (edit == null && !preview && !readonly) Modifier.clickable { state.editCell(row.id, column.key) } else Modifier
    ) {
        if (edit == null) column.type.Display(row[column.key], Modifier.fillMaxWidth())
        else DataTableEditor(edit, Modifier.fillMaxSize())
    }
}

@Composable
private fun DataTableBodyRow(
    state: DataTableState,
    row: DataTableRow,
    index: Int,
    widths: DataTableWidths,
    horizontal: ScrollState,
    gutter: Dp,
    height: Dp,
    readonly: Boolean,
    modifier: Modifier = Modifier,
    preview: Boolean = false,
    dropTarget: Boolean = false,
    dropAfter: Boolean = false,
) {
    val focus = LocalFocusManager.current
    val background = if (preview || index % 2 == 0) Theme.color.surface else Theme.color.backgroundVariant
    val highlight = Theme.color.primary
    val border = Theme.border.v5

    Row(modifier.background(background).drawWithContent {
        drawContent()
        if (dropTarget) {
            val stroke = border.toPx()
            val y = if (dropAfter) size.height - stroke / 2 else stroke / 2
            drawLine(highlight, Offset(0f, y), Offset(size.width, y), stroke)
        }
        if (preview) drawRect(highlight, style = Stroke(border.toPx()))
    }) {
        // 可编辑模式的指针点击由列表手势处理，此处同时提供只读模式和无障碍的点击入口。
        DataTableCellFrame(gutter, height, modifier = if (preview) Modifier else Modifier.clickable { state.clearSelection(); focus.clearFocus() }) {
            SimpleEllipsisText(
                (index + 1).toString().padStart(2, '0'),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.h / 2).then(if (preview || readonly) Modifier else Modifier.semantics {
                    contentDescription = if (state.sortDirection == DataTableSortDirection.None) "第 ${index + 1} 行，拖动调整顺序，长按删除" else "第 ${index + 1} 行，长按删除"
                    onLongClick(label = "删除此行") { state.clearSelection(); focus.clearFocus(); state.removeRow(row.id); true }
                }),
                textAlign = TextAlign.Center,
                style = Theme.typography.v9.copy(fontFamily = FontFamily.Monospace),
                color = Theme.color.onSurfaceVariant,
            )
        }
        Row(Modifier.weight(1f).horizontalScroll(horizontal).width(widths.total)) {
            for (column in state.columns) key(column.key) {
                DataTableBodyCell(state, row, column, widths.getValue(column.key), height, readonly, preview)
            }
        }
    }
}

/**
 * 数据表编辑器。点击数据单元格直接内嵌编辑，合法输入即时保存，bool 单击切换，selection 使用单元格内的下拉入口。
 * 第一列固定为 # 和序号，未排序时拖动序号调整行顺序，长按删除；直接拖动只读表头调整数据列顺序。
 * 单击序号列或表头行的任意单元格清除焦点、结束编辑并取消选中。
 * Enter 保存，Escape 恢复编辑前的值，两者均结束编辑、清除焦点并取消选中。
 * 表头右侧按钮循环切换未排序、升序、降序，末行增加数据。
 * [readonly] 关闭单元格编辑、行增删和行列拖动，仍可排序和滚动查看。
 * 外部固定尺寸（包括 fillMaxWidth/fillMaxHeight）优先；默认宽 `Theme.size.cell1`、高为实际宽的一半。
 */
@Composable
fun DataTable(state: DataTableState, modifier: Modifier = Modifier, readonly: Boolean = false) {
    val density = LocalDensity.current
    val focus = LocalFocusManager.current
    val window = LocalWindowInfo.current
    val scope = rememberCoroutineScope()
    val list = remember(state) { LazyListState() }
    val horizontal = rememberScrollState()
    val drag = remember(state) { DataTableDragState() }
    val defaultWidth = Theme.size.cell1
    val rowHeight = Theme.size.icon + Theme.padding.v * 4
    val headerHeight = rowHeight + Theme.padding.v
    val gutter = Theme.size.cell10.coerceAtLeast(Theme.size.icon + Theme.padding.h * 2)
    val minColumnWidth = Theme.size.icon + Theme.padding.h * 4
    val defaultColumnWidth = Theme.size.cell3
    val shape = Theme.shape.v7
    val currentReadonly by rememberUpdatedState(readonly)

    DisposableEffect(state) { onDispose { state.commitEdit() } }
    LaunchedEffect(state, window) {
        snapshotFlow { window.isWindowFocused }.collect { if (!it) state.commitEdit() }
    }
    LaunchedEffect(state, readonly) {
        if (readonly) {
            val hadEditing = state.editing != null
            state.clearSelection()
            if (hadEditing) focus.clearFocus()
            drag.clear()
        }
    }

    BoxWithConstraints(
        modifier = modifier.layout { measurable, constraints ->
            val width = constraints.constrainWidth(defaultWidth.roundToPx())
            val height = constraints.constrainHeight((width / 2f).roundToInt())
            val placeable = measurable.measure(Constraints.fixed(width, height))
            layout(width, height) { placeable.placeRelative(0, 0) }
        }.clip(shape).background(Theme.color.surface).border(Theme.border.v7, Theme.color.outline, shape),
        propagateMinConstraints = true,
    ) {
        val columns = state.columns.toList()
        val widths = remember(columns, maxWidth, gutter, minColumnWidth, defaultColumnWidth) {
            val base = columns.associate { it.key to (if (it.width == Dp.Unspecified) defaultColumnWidth else it.width).coerceAtLeast(minColumnWidth) }
            val total = gutter + base.values.fold(0.dp) { sum, width -> sum + width }
            val extra = (maxWidth - total).coerceAtLeast(0.dp) / columns.size
            DataTableWidths(base.mapValues { it.value + extra })
        }
        val gutterPx = with(density) { gutter.toPx() }
        val viewportWidth = (constraints.maxWidth - gutterPx).coerceAtLeast(0f)
        val edge = with(density) { rowHeight.toPx() }

        LaunchedEffect(drag.columnKey, readonly) {
            while (!readonly && drag.columnKey != null) {
                withFrameNanos { }
                if (drag.columnKey == null) break
                val pointer = drag.columnPointer - horizontal.value
                val margin = minOf(edge, viewportWidth / 3)
                val delta = when {
                    pointer < margin -> (pointer - margin) * 0.2f
                    pointer > viewportWidth - margin -> (pointer - viewportWidth + margin) * 0.2f
                    else -> 0f
                }.coerceIn(-edge / 2, edge / 2)
                if (delta != 0f) {
                    val consumed = horizontal.scrollBy(delta)
                    drag.columnOffset += consumed
                    drag.columnPointer += consumed
                    drag.updateColumnTarget(state.columns, widths, density.density)
                }
            }
        }

        LaunchedEffect(drag.rowId, readonly) {
            while (!readonly && drag.rowId != null) {
                withFrameNanos { }
                if (drag.rowId == null) break
                val end = list.layoutInfo.viewportEndOffset.toFloat()
                val margin = minOf(edge, end / 3)
                val delta = when {
                    drag.rowPointer < margin -> (drag.rowPointer - margin) * 0.2f
                    drag.rowPointer > end - margin -> (drag.rowPointer - end + margin) * 0.2f
                    else -> 0f
                }.coerceIn(-edge / 2, edge / 2)
                if (delta != 0f) {
                    list.scrollBy(delta)
                    drag.updateRowTarget(list, state.rowCount)
                }
            }
        }

        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().height(headerHeight).background(Theme.color.backgroundVariant).zIndex(1f)) {
                    DataTableCellFrame(gutter, headerHeight, modifier = Modifier.clickable { state.clearSelection(); focus.clearFocus() }) {
                        SimpleEllipsisText("#", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = Theme.typography.v8.bold, color = Theme.color.onSurfaceVariant)
                    }
                    Row(Modifier.weight(1f).horizontalScroll(horizontal).width(widths.total)) {
                        for (column in columns) key(column.key) {
                            DataTableHeaderCell(state, column, widths, headerHeight, drag, readonly)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                    LazyColumn(
                        state = list,
                        modifier = Modifier.fillMaxSize().then(if (readonly) Modifier else Modifier.dataTableRowGestures(state, list, drag, gutterPx) { state.clearSelection(); focus.clearFocus(); drag.clear() })
                    ) {
                        itemsIndexed(state.rows, key = { _, row -> row.id }, contentType = { _, _ -> "row" }) { index, row ->
                            DataTableBodyRow(
                                state, row, index, widths, horizontal, gutter, rowHeight,
                                readonly = readonly,
                                modifier = Modifier.fillMaxWidth().alpha(if (drag.rowId == row.id) 0.3f else 1f),
                                dropTarget = drag.rowId != null && drag.targetIndex == index,
                                dropAfter = drag.rowOriginIndex < index,
                            )
                        }
                        if (!readonly) item(key = "data-table:add", contentType = "add") {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(rowHeight).background(Theme.color.primary.copy(alpha = 0.06f))
                                    .clickable {
                                        focus.clearFocus()
                                        val row = state.addRow()
                                        scope.launch {
                                            list.scrollToItem(state.rows.indexOf(row))
                                            val column = state.columns.firstOrNull { it.type !== DataTableCellType.Bool }
                                            if (!currentReadonly && column != null) state.editCell(row.id, column.key)
                                        }
                                    }.padding(horizontal = Theme.padding.h),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                            ) {
                                Icon(Icons.Add, color = Theme.color.primary, modifier = Modifier.size(Theme.size.smallIcon))
                                SimpleEllipsisText("添加一行", style = Theme.typography.v8, color = Theme.color.primary)
                            }
                        }
                    }
                    val draggedRow = drag.rowId?.let { state.findRow(it) }
                    if (draggedRow != null) {
                        DataTableBodyRow(
                            state, draggedRow, drag.rowOriginIndex, widths, horizontal, gutter, rowHeight,
                            readonly = readonly,
                            modifier = Modifier.fillMaxWidth().offset { IntOffset(0, (drag.rowPointer - drag.rowGrabOffset).roundToInt()) }.zIndex(2f),
                            preview = true,
                        )
                    }
                }
            }
        }
    }
}
