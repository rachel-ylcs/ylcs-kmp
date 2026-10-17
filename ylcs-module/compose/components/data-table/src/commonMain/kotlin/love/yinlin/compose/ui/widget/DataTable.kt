package love.yinlin.compose.ui.widget

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.extension.cleaning
import kotlin.math.roundToInt

@Composable
private fun DataTableEditor(state: DataTableEditState, modifier: Modifier) {
    val requester = remember(state) { FocusRequester() }
    var hadFocus by rememberFalse(state)

    Box(modifier = modifier.focusRequester(requester).onFocusChanged {
        if (it.hasFocus) hadFocus = true
        else if (hadFocus) state.commit()
    }.focusGroup()) {
        state.type.Editor(state = state, modifier = Modifier.fillMaxSize())
    }

    LaunchedEffect(state) {
        requester.requestFocus()
        snapshotFlow { state.text }.collect { state.onInput(it) }
    }

    DisposableEffect(state) {
        onDispose(state::commit)
    }
}

private fun Modifier.dataTableRowGestures(
    state: DataTableState<*>,
    list: LazyListState,
    drag: DataTableDragState,
    gutter: Float,
    onStart: () -> Unit,
): Modifier = pointerInput(state, list, gutter, state.sortDirection == DataTableSortDirection.None, state.structureVersion) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (down.position.x !in 0f ..< gutter) return@awaitEachGesture
        val item = list.layoutInfo.visibleItemsInfo.firstOrNull { down.position.y >= it.offset && down.position.y < it.offset + it.size } ?: return@awaitEachGesture
        val rowId = item.key as? Long ?: return@awaitEachGesture

        down.consume()
        val slop = if (down.type == PointerType.Mouse) viewConfiguration.touchSlop / 144f else viewConfiguration.touchSlop
        var start: PointerInputChange? = null

        while (start == null) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
            if (change.isConsumed || event.changes.any { it.id != down.id && it.pressed }) return@awaitEachGesture
            change.consume()
            if (!change.pressed) {
                if (change.position.x in 0f ..< gutter && change.position.y >= item.offset && change.position.y < item.offset + item.size) onStart()
                return@awaitEachGesture
            }
            if ((change.position - down.position).getDistance() > slop) start = change
        }

        onStart()
        if (state.sortDirection != DataTableSortDirection.None) return@awaitEachGesture
        drag.rowId = rowId
        drag.rowGrabOffset = down.position.y - item.offset
        drag.rowOriginIndex = state.indexOfRow(rowId)
        drag.rowPointer = start.position.y
        drag.targetIndex = item.index

        cleaning(drag::clear) {
            var dropped = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed || state.sortDirection != DataTableSortDirection.None) break
                change.consume()
                drag.rowPointer = change.position.y
                drag.updateRowTarget(list, state.rowCount)
                if (!change.pressed) {
                    dropped = true
                    break
                }
            }
            if (dropped && drag.targetIndex in 0 until state.rowCount) state.moveRowById(rowId, drag.targetIndex)
        }
    }
}

@Composable
private fun DataTableCellFrame(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    invalid: Boolean = false,
    background: Color = Colors.Transparent,
    content: @Composable BoxScope.() -> Unit,
) {
    val outline = Theme.color.outline.copy(alpha = 0.35f)
    val highlight = if (invalid) Theme.color.error else Theme.color.primary
    val selectedBackground = Theme.color.primary.copy(alpha = 0.12f)
    val line = Theme.border.v8
    val selectionLine = Theme.border.v6

    Box(
        modifier = modifier.width(width).height(height).background(background)
            .background(if (isSelected) selectedBackground else Colors.Transparent)
            .drawWithContent {
                drawContent()
                val stroke = line.toPx()
                drawLine(outline, Offset(size.width - stroke / 2, 0f), Offset(size.width - stroke / 2, size.height), stroke)
                drawLine(outline, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
                if (isSelected || invalid) {
                    val border = selectionLine.toPx()
                    drawRect(
                        color = highlight,
                        topLeft = Offset(border / 2, border / 2),
                        size = size.copy(width = (size.width - border).coerceAtLeast(0f),
                        height = (size.height - border).coerceAtLeast(0f)),
                        style = Stroke(border)
                    )
                }
            },
        contentAlignment = Alignment.CenterStart,
        content = content
    )
}

@Composable
private fun DataTableHeaderCell(
    state: DataTableState<out DataTableItem>,
    column: DataTableColumnEntry,
    widths: DataTableWidths,
    height: Dp,
    drag: DataTableDragState,
    readonly: Boolean,
) {
    val density = LocalDensity.current
    val focus = LocalFocusManager.current
    val dragging = drag.columnKey == column.key
    val target = drag.columnKey != null && state.columnList.getOrNull(drag.targetIndex)?.key == column.key

    DataTableCellFrame(
        width = widths[column.key],
        height = height,
        isSelected = target || dragging,
        background = Theme.color.backgroundVariant,
        modifier = Modifier
            .offset { IntOffset(if (dragging) drag.columnOffset.roundToInt() else 0, 0) }
            .zIndex(if (dragging) 2f else 0f)
            .silentClick {
                state.clearSelection()
                focus.clearFocus()
            }.then(if (readonly) Modifier else Modifier.pointerInput(state, column.key, widths, density) {
                detectDragGestures(
                    onDragStart = { pointer ->
                        state.clearSelection()
                        focus.clearFocus()
                        drag.clear()
                        drag.columnKey = column.key
                        val left = state.columnList.takeWhile { it.key != column.key }.sumOf { widths[it.key].value.toDouble() }.toFloat()
                        drag.columnPointer = with(density) { left.dp.toPx() } + pointer.x
                        drag.updateColumnTarget(state.columnList, widths, density.density)
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        drag.columnOffset += delta.x
                        drag.columnPointer += delta.x
                        drag.updateColumnTarget(state.columnList, widths, density.density)
                    },
                    onDragEnd = {
                        if (drag.targetIndex in state.columnList.indices) state.moveColumnById(column.key, drag.targetIndex)
                        drag.clear()
                    },
                    onDragCancel = drag::clear
                )
            }),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText(text = column.title, modifier = Modifier.weight(1f), style = Theme.typography.v8.bold)

            if (column.canSort) {
                val sorted = state.sortColumnKey == column.key
                Icon(
                    icon = when {
                        !sorted -> Icons.SwapVert
                        state.sortDirection == DataTableSortDirection.Ascending -> Icons.ArrowUpward
                        else -> Icons.ArrowDownward
                    },
                    color = if (sorted) Theme.color.primary else Theme.color.onSurfaceVariant,
                    modifier = Modifier.size(Theme.size.smallIcon),
                    onClick = {
                        state.clearSelection()
                        focus.clearFocus()
                        state.sortById(column.key)
                    }
                )
            }
        }
    }
}

@Composable
private fun DataTableBodyCell(
    state: DataTableState<out DataTableItem>,
    row: DataTableItem,
    rowId: Long,
    column: DataTableColumnEntry,
    width: Dp,
    height: Dp,
    readonly: Boolean,
    preview: Boolean
) {
    val cell = remember(rowId, column.key) { DataTableCellAddress(rowId, column.key) }
    val value = state.cellOrNull(row, column.key)
    val selected by rememberDerivedState(state, cell) { state.selectedAddress == cell}
    val editing by rememberDerivedState(state, cell) { state.editing?.takeIf { it.cell == cell } }
    val edit = if (preview || readonly) null else editing

    DataTableCellFrame(
        width = width,
        height = height,
        isSelected = selected && !preview,
        invalid = !preview && (edit?.error == true || state.invalidCell == cell),
        modifier = Modifier.condition(value != null && edit == null && !preview && !readonly) {
            clickable { state.editCellById(rowId, column.key) }
        }
    ) {
        if (edit == null) value?.Display(modifier = Modifier.fillMaxWidth())
        else DataTableEditor(state = edit, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun DataTableBodyRow(
    state: DataTableState<out DataTableItem>,
    row: DataTableItem,
    rowId: Long,
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

    Row(modifier = modifier.background(background).drawWithContent {
        drawContent()
        if (dropTarget) {
            val stroke = border.toPx()
            val y = if (dropAfter) size.height - stroke / 2 else stroke / 2
            drawLine(highlight, Offset(0f, y), Offset(size.width, y), stroke)
        }
        if (preview) drawRect(highlight, style = Stroke(border.toPx()))
    }) {
        DataTableCellFrame(
            width = gutter,
            height = height,
            modifier = if (preview) Modifier else Modifier.silentClick {
                state.clearSelection()
                focus.clearFocus()
            }
        ) {
            SimpleEllipsisText(
                text = (index + 1).toString().padStart(2, '0'),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.h / 2),
                textAlign = TextAlign.Center,
                style = Theme.typography.v9,
                color = Theme.color.onSurfaceVariant,
            )
        }

        Row(modifier = Modifier.weight(1f).horizontalScroll(horizontal).width(widths.total)) {
            for (column in state.columnList) key(column.key) {
                DataTableBodyCell(state, row, rowId, column, widths[column.key], height, readonly, preview)
            }
        }
    }
}

/**
 * 数据表编辑器。点击数据单元格直接内嵌编辑，合法输入即时保存，bool 单击切换，selection 使用单元格内的下拉入口。
 * 第一列固定为 # 和序号，未排序时拖动序号调整行顺序；直接拖动只读表头调整数据列顺序。
 * Enter 保存，Escape 恢复编辑前的值，两者均结束编辑、清除焦点并取消选中。
 * 列中存在实现 DataTableSortableCell 的单元格时显示排序按钮，循环切换未排序、升序、降序并回到首行
 *
 * @param readonly 关闭单元格编辑和行列拖动，仍可排序和滚动查看。
 */
@Composable
fun <T : DataTableItem> DataTable(
    state: DataTableState<T>,
    modifier: Modifier = Modifier,
    readonly: Boolean = false
) {
    val density = LocalDensity.current
    val focus = LocalFocusManager.current
    val window = LocalWindowInfo.current
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

    DisposableEffect(state) {
        onDispose { state.commitEdit() }
    }

    LaunchedEffect(state, window) {
        snapshotFlow { window.isWindowFocused }.collect {
            if (!it) state.commitEdit()
        }
    }

    LaunchedEffect(state, readonly) {
        if (readonly) {
            val hadEditing = state.editing != null
            state.clearSelection()
            if (hadEditing) focus.clearFocus()
            drag.clear()
        }
    }

    LaunchedEffect(state, state.sortVersion) {
        list.requestScrollToItem(index = 0, scrollOffset = 0)
    }

    LaunchedEffect(state, state.structureVersion) {
        drag.clear()
    }

    BoxWithConstraints(
        modifier = modifier.layout { measurable, constraints ->
            val width = constraints.constrainWidth(defaultWidth.roundToPx())
            val height = constraints.constrainHeight((width / 2f).roundToInt())
            val placeable = measurable.measure(Constraints.fixed(width, height))
            layout(width, height) { placeable.placeRelative(0, 0) }
        }.clip(shape).background(Theme.color.surface).border(Theme.border.v7, Theme.color.outline, shape),
        propagateMinConstraints = true
    ) {
        val columns = state.columnList
        val [rows, rowIds] = state.rowSnapshot()
        val widths = remember(columns, maxWidth, gutter, minColumnWidth, defaultColumnWidth) {
            val base = columns.associate { it.key to (if (it.width == Dp.Unspecified) defaultColumnWidth else it.width).coerceAtLeast(minColumnWidth) }
            val total = gutter + base.values.fold(0.dp) { sum, width -> sum + width }
            val extra = if (columns.isEmpty()) 0.dp else (maxWidth - total).coerceAtLeast(0.dp) / columns.size
            DataTableWidths(base.mapValues { it.value + extra })
        }
        val [gutterPx, edge] = with(density) { gutter.toPx() to rowHeight.toPx() }
        val viewportWidth = (constraints.maxWidth - gutterPx).coerceAtLeast(0f)

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
                    drag.updateColumnTarget(state.columnList, widths, density.density)
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

        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().height(headerHeight).background(Theme.color.backgroundVariant).zIndex(1f)) {
                    DataTableCellFrame(
                        width = gutter,
                        height = headerHeight,
                        modifier = Modifier.silentClick {
                            state.clearSelection()
                            focus.clearFocus()
                        }
                    ) {
                        SimpleEllipsisText(
                            text = "#",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = Theme.typography.v8.bold,
                            color = Theme.color.onSurfaceVariant
                        )
                    }
                    Row(modifier = Modifier.weight(1f).horizontalScroll(horizontal).width(widths.total)) {
                        for (column in columns) key(column.key) {
                            DataTableHeaderCell(state, column, widths, headerHeight, drag, readonly)
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                    LazyColumn(
                        state = list,
                        modifier = Modifier.fillMaxSize().condition(!readonly) {
                            dataTableRowGestures(state, list, drag, gutterPx) {
                                state.clearSelection()
                                focus.clearFocus()
                                drag.clear()
                            }
                        }
                    ) {
                        itemsIndexed(
                            items = rows,
                            key = { index, _ -> rowIds[index] },
                            contentType = { _, _ -> "row" }
                        ) { index, row ->
                            val rowId = rowIds[index]
                            DataTableBodyRow(
                                state, row, rowId, index, widths, horizontal, gutter, rowHeight,
                                readonly = readonly,
                                modifier = Modifier.fillMaxWidth().alpha(if (drag.rowId == rowId) 0.3f else 1f),
                                dropTarget = drag.rowId != null && drag.targetIndex == index,
                                dropAfter = drag.rowOriginIndex < index
                            )
                        }
                    }
                    val draggedId = drag.rowId
                    val draggedRow = draggedId?.let { state.findRow(it) }
                    if (draggedRow != null) {
                        DataTableBodyRow(
                            state, draggedRow, draggedId, drag.rowOriginIndex, widths, horizontal, gutter, rowHeight,
                            readonly = readonly,
                            modifier = Modifier.fillMaxWidth().offset {
                                IntOffset(0, (drag.rowPointer - drag.rowGrabOffset).roundToInt())
                            }.zIndex(2f),
                            preview = true
                        )
                    }
                }
            }
        }
    }
}
