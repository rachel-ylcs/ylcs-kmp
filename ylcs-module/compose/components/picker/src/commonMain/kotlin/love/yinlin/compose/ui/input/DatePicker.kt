package love.yinlin.compose.ui.input

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import kotlinx.datetime.*
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.layout.HorizontalScrollContainer
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.text.SimpleClipText

private const val DATE_PICKER_CELL_PADDING_RATIO = 0.2f
private const val DATE_PICKER_INITIAL_YEAR_INDEX = 4

private fun TextLayoutResult.centerInCell(index: Int, columns: Int, cellWidth: Float, cellHeight: Float): Offset = Offset(
    x = index % columns * cellWidth + (cellWidth - size.width) / 2f,
    y = index / columns * cellHeight + (cellHeight - size.height) / 2f
)

@Composable
private fun DatePickerWeekRow(width: Dp, cellSize: Dp, textLayouts: DatePickerTextLayouts) {
    val color = LocalColor.current
    val labels = remember { arrayOfNulls<TextLayoutResult>(7) }
    val positions = remember { LongArray(labels.size) }

    Spacer(modifier = Modifier.size(width, cellSize).drawWithCache {
        val cellWidth = size.width / 7f
        val cellHeight = size.height

        for (index in labels.indices) {
            val label = textLayouts.weekday(index)
            labels[index] = label
            positions[index] = label.centerInCell(index, 7, cellWidth, cellHeight).packedValue
        }

        onDrawBehind {
            for (index in labels.indices) {
                val label = labels[index]
                if (label != null) drawText(label, color = color, topLeft = Offset(positions[index]))
            }
        }
    })
}

@Composable
private fun DatePickerDayGrid(
    month: YearMonth,
    state: DatePickerState,
    width: Dp,
    cellSize: Dp,
    textLayouts: DatePickerTextLayouts,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDay = month.firstDay
    val firstDayIndex = firstDay.dayOfWeek.isoDayNumber - 1
    val start = firstDay.minus(firstDayIndex, DateTimeUnit.DAY)
    val startEpochDay = start.toEpochDays()

    val selectedIndex by rememberDerivedState(startEpochDay, state) {
        val offset = state.date?.toEpochDays()?.minus(startEpochDay) ?: -1L
        if (offset in 0L ..< 42L) offset.toInt() else -1
    }

    val normalColor = LocalColor.current
    val otherColor = LocalColorVariant.current.copy(alpha = 0.5f)
    val selectedBackground = Theme.color.primaryContainer
    val selectedColor = Theme.color.onContainer
    val selectedShape = Theme.shape.v8
    val currentOnDateSelected by rememberUpdatedState(onDateSelected)

    val labels = remember { arrayOfNulls<TextLayoutResult>(42) }
    val positions = remember { LongArray(labels.size) }

    Spacer(modifier = Modifier.size(width, cellSize * 6).pointerInput(start) {
        detectTapGestures { point ->
            val column = (point.x / size.width * 7).toInt().coerceIn(0, 6)
            val row = (point.y / size.height * 6).toInt().coerceIn(0, 5)
            currentOnDateSelected(start.plus(row * 7 + column, DateTimeUnit.DAY))
        }
    }.drawWithCache {
        val cellWidth = size.width / 7f
        val cellHeight = size.height / 6f
        val firstVisibleDay = start.day
        val nextMonthIndex = firstDayIndex + month.numberOfDays
        val selectionOutline = selectedShape.createOutline(Size(cellWidth, cellHeight), layoutDirection, this)

        for (index in labels.indices) {
            val day = when {
                index < firstDayIndex -> firstVisibleDay + index
                index < nextMonthIndex -> index - firstDayIndex + 1
                else -> index - nextMonthIndex + 1
            }
            val label = textLayouts.day(day)
            labels[index] = label
            positions[index] = label.centerInCell(index, 7, cellWidth, cellHeight).packedValue
        }

        onDrawBehind {
            val selection = selectedIndex
            if (selection >= 0) {
                translate(selection % 7 * cellWidth, selection / 7 * cellHeight) {
                    drawOutline(selectionOutline, selectedBackground)
                }
            }

            for (index in labels.indices) {
                val color = when (index) {
                    selection -> selectedColor
                    in firstDayIndex ..< nextMonthIndex -> normalColor
                    else -> otherColor
                }
                val label = labels[index]
                if (label != null) drawText(label, color = color, topLeft = Offset(positions[index]))
            }
        }
    })
}

@Composable
private fun DatePickerChoiceGrid(
    width: Dp,
    height: Dp,
    textLayouts: DatePickerTextLayouts,
    selectedIndex: Int,
    firstYear: Int? = null,
    onChoice: (Int) -> Unit
) {
    val normalColor = LocalColor.current
    val selectedColor = Theme.color.primary
    val currentCallback by rememberUpdatedState(onChoice)

    val labels = remember { arrayOfNulls<TextLayoutResult>(12) }
    val positions = remember { LongArray(labels.size) }

    Box(modifier = Modifier.size(width, height).pointerInput(Unit) {
        detectTapGestures { point ->
            val column = (point.x / size.width * 3).toInt().coerceIn(0, 2)
            val row = (point.y / size.height * 4).toInt().coerceIn(0, 3)
            currentCallback(row * 3 + column)
        }
    }.drawWithCache {
        val cellWidth = size.width / 3f
        val cellHeight = size.height / 4f

        for (index in labels.indices) {
            val label = if (firstYear == null) textLayouts.month(index) else textLayouts.year(firstYear + index)
            labels[index] = label
            positions[index] = label.centerInCell(index, 3, cellWidth, cellHeight).packedValue
        }

        onDrawBehind {
            for (index in labels.indices) {
                val color = if (index == selectedIndex) selectedColor else normalColor
                val label = labels[index]
                if (label != null) drawText(label, color = color, topLeft = Offset(positions[index]))
            }
        }
    })
}

@Composable
private fun DatePickerDaysHeader(
    state: DatePickerState,
    width: Dp,
    cellSize: Dp,
    textHeight: Dp,
    style: TextStyle
) {
    val pager = state.monthPager
    val month = state.monthAt(pager.currentPage)

    Row(
        modifier = Modifier.size(width, cellSize),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon = Icons.KeyboardArrowLeft,
            modifier = Modifier.size(textHeight),
            onClick = {
                val target = pager.currentPage - 1
                if (target >= 0) pager.requestScrollToPage(target)
            }
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleClipText(
                text = "${month.year}年",
                style = style,
                modifier = Modifier.pointerIcon(PointerIcon.Hand).silentClick { state.view = DatePickerView.Years }
            )

            SimpleClipText(
                text = "${month.month.number}月",
                style = style,
                modifier = Modifier.pointerIcon(PointerIcon.Hand).silentClick { state.view = DatePickerView.Months }
            )
        }

        Icon(
            icon = Icons.KeyboardArrowRight,
            modifier = Modifier.size(textHeight),
            onClick = {
                val target = pager.currentPage + 1
                if (target < pager.pageCount) pager.requestScrollToPage(target)
            }
        )
    }
}

@Composable
private fun DatePickerDaysView(
    state: DatePickerState,
    width: Dp,
    cellSize: Dp,
    textHeight: Dp,
    style: TextStyle,
    textLayouts: DatePickerTextLayouts
) {
    val pager = state.monthPager

    Column {
        DatePickerDaysHeader(state, width, cellSize, textHeight, style)
        DatePickerWeekRow(width, cellSize, textLayouts)

        HorizontalScrollContainer(state = pager, modifier = Modifier.size(width, cellSize * 6)) {
            HorizontalPager(state = pager, beyondViewportPageCount = 1, key = { it }) { page ->
                DatePickerDayGrid(
                    month = state.monthAt(page),
                    state = state,
                    width = width,
                    cellSize = cellSize,
                    textLayouts = textLayouts,
                    onDateSelected = { state.select(it) }
                )
            }
        }
    }
}

@Composable
private fun DatePickerYearsView(
    month: YearMonth,
    width: Dp,
    height: Dp,
    textLayouts: DatePickerTextLayouts,
    onMonthChanged: (YearMonth) -> Unit
) {
    val pager = rememberPagerState(initialPage = DatePickerState.CENTER_PAGE) { Int.MAX_VALUE }
    val firstYear = month.year - DATE_PICKER_INITIAL_YEAR_INDEX

    HorizontalScrollContainer(state = pager, modifier = Modifier.size(width, height)) {
        HorizontalPager(state = pager, beyondViewportPageCount = 1, key = { it }) { page ->
            val year = firstYear + (page - DatePickerState.CENTER_PAGE) * 12

            DatePickerChoiceGrid(
                width = width,
                height = height,
                textLayouts = textLayouts,
                selectedIndex = if (page == DatePickerState.CENTER_PAGE) DATE_PICKER_INITIAL_YEAR_INDEX else -1,
                firstYear = year,
                onChoice = { onMonthChanged(YearMonth(year + it, month.month)) }
            )
        }
    }
}

@Composable
private fun DatePickerMonthsView(
    month: YearMonth,
    width: Dp,
    height: Dp,
    textLayouts: DatePickerTextLayouts,
    onMonthChanged: (YearMonth) -> Unit
) {
    val pager = rememberPagerState(initialPage = DatePickerState.CENTER_PAGE) { Int.MAX_VALUE }

    HorizontalScrollContainer(state = pager, modifier = Modifier.size(width, height)) {
        HorizontalPager(state = pager, beyondViewportPageCount = 1, key = { it }) { page ->
            val year = month.year + (page - DatePickerState.CENTER_PAGE)

            DatePickerChoiceGrid(
                width = width,
                height = height,
                textLayouts = textLayouts,
                selectedIndex = if (page == DatePickerState.CENTER_PAGE) month.month.number - 1 else -1,
                onChoice = { onMonthChanged(YearMonth(year, it + 1)) }
            )
        }
    }
}

@Composable
fun DatePicker(
    state: DatePickerState,
    modifier: Modifier = Modifier,
    style: TextStyle = Theme.typography.v6
) {
    val measurer = rememberTextMeasurer(cacheSize = 36)
    val textLayouts = remember(measurer, style) { DatePickerTextLayouts(measurer, style) }

    val density = LocalDensity.current
    val textHeight = with(density) { textLayouts.day(31).size.height.toDp() }

    val cellPadding = textHeight * DATE_PICKER_CELL_PADDING_RATIO
    val cellSize = textHeight + cellPadding * 2
    val gridWidth = cellSize * 7
    val totalHeight = cellSize * 8

    Box(modifier = modifier.size(gridWidth, totalHeight)) {
        when (state.view) {
            DatePickerView.Days -> DatePickerDaysView(
                state = state,
                width = gridWidth,
                cellSize = cellSize,
                textHeight = textHeight,
                style = style,
                textLayouts = textLayouts
            )
            DatePickerView.Years -> DatePickerYearsView(
                month = state.monthAt(state.monthPager.currentPage),
                width = gridWidth,
                height = totalHeight,
                textLayouts = textLayouts,
                onMonthChanged = state::select
            )
            DatePickerView.Months -> DatePickerMonthsView(
                month = state.monthAt(state.monthPager.currentPage),
                width = gridWidth,
                height = totalHeight,
                textLayouts = textLayouts,
                onMonthChanged = state::select
            )
        }
    }
}
