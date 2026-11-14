package love.yinlin.compose.ui.input

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.layout.HorizontalScrollContainer
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.measureTextHeight
import love.yinlin.extension.DateEx

/** 日期选择器当前显示的面板。可用于指定初始预览面板。 */
@Stable
enum class DatePickerView { Days, Years, Months }

private const val DatePickerCenterPage = Int.MAX_VALUE / 2
private const val DatePickerCellPaddingRatio = 0.1f

private fun DrawScope.drawDatePickerText(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    color: Color,
    x: Float,
    y: Float,
    width: Float,
    height: Float
) {
    val layout = measurer.measure(text = text, style = style, maxLines = 1)
    drawText(layout, color = color, topLeft = Offset(
        x + (width - layout.size.width) / 2f,
        y + (height - layout.size.height) / 2f
    ))
}

@Composable
private fun DatePickerWeekRow(width: Dp, cellSize: Dp, style: TextStyle, measurer: TextMeasurer) {
    val color = LocalColor.current

    Canvas(modifier = Modifier.size(width, cellSize)) {
        val cell = size.width / 7f
        "一二三四五六日".forEachIndexed { index, day ->
            drawDatePickerText(measurer, day.toString(), style, color, index * cell, 0f, cell, size.height)
        }
    }
}

@Composable
private fun DatePickerDays(
    month: LocalDate,
    selected: LocalDate?,
    width: Dp,
    cellSize: Dp,
    style: TextStyle,
    measurer: TextMeasurer,
    onDateChanged: (LocalDate) -> Unit
) {
    val start = month.minus(month.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    val normalColor = LocalColor.current
    val otherColor = LocalColorVariant.current
    val selectedColor = Theme.color.primary
    val currentCallback by rememberUpdatedState(onDateChanged)

    Canvas(modifier = Modifier.size(width, cellSize * 6).pointerInput(start) {
        detectTapGestures { point ->
            val column = (point.x / size.width * 7).toInt().coerceIn(0, 6)
            val row = (point.y / size.height * 6).toInt().coerceIn(0, 5)
            val clicked = start.plus(row * 7 + column, DateTimeUnit.DAY)
            currentCallback(clicked)
        }
    }) {
        val cell = size.width / 7f

        for (index in 0 ..< 42) {
            val day = start.plus(index, DateTimeUnit.DAY)
            val color = when {
                day == selected -> selectedColor
                day.year == month.year && day.month == month.month -> normalColor
                else -> otherColor
            }

            drawDatePickerText(
                measurer = measurer,
                text = day.day.toString(),
                style = style,
                color = color,
                x = index % 7 * cell,
                y = index / 7 * cell,
                width = cell,
                height = cell
            )
        }
    }
}

@Composable
private fun DatePickerChoiceGrid(
    width: Dp,
    height: Dp,
    style: TextStyle,
    measurer: TextMeasurer,
    labels: List<String>,
    selectedIndex: Int,
    onChoice: (Int) -> Unit
) {
    val normalColor = LocalColor.current
    val selectedColor = Theme.color.primary
    val currentCallback by rememberUpdatedState(onChoice)

    Canvas(modifier = Modifier.size(width, height).pointerInput(labels) {
        detectTapGestures { point ->
            val column = (point.x / size.width * 3).toInt().coerceIn(0, 2)
            val row = (point.y / size.height * 4).toInt().coerceIn(0, 3)
            currentCallback(row * 3 + column)
        }
    }) {
        val cellWidth = size.width / 3f
        val cellHeight = size.height / 4f

        for (index in 0 ..< 12) {
            drawDatePickerText(
                measurer = measurer,
                text = labels[index],
                style = style,
                color = if (index == selectedIndex) selectedColor else normalColor,
                x = index % 3 * cellWidth,
                y = index / 3 * cellHeight,
                width = cellWidth,
                height = cellHeight
            )
        }
    }
}

/**
 * 固定六行、周一开始的日期选择器。尺寸由 [style] 的“31”文本高度决定：
 * 日期格为正方形，文本四周留出按文本高度 10% 计算的内间距。
 * 七列宽度与星期条一致，顶部年份条和星期条各占一个带内间距的单元高度。
 *
 * 左右箭头切换年份，横向滑动或鼠标纵向滚轮切换月份。年份、月份选择面板
 * 占用整个组件区域；调整浏览年份或月份不会调用 [onDateChanged]，
 * 点击任意日期都会调用 [onDateChanged]，即使与当前选中日期相同。
 * [date] 为空时默认展示当前月份，不高亮任何日期。
 *
 * @param initialView 初始面板；切换年份、月份时不会修改外部选中日期。
 */
@Composable
fun DatePicker(
    date: LocalDate? = null,
    onDateChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = Theme.typography.v8,
    initialView: DatePickerView = DatePickerView.Days
) {
    val textHeight = measureTextHeight("31", style)
    val cellPadding = textHeight * DatePickerCellPaddingRatio
    val cellSize = textHeight + cellPadding * 2
    val gridWidth = cellSize * 7
    val totalHeight = cellSize * 8
    val measurer = rememberTextMeasurer(cacheSize = 96)
    val scope = rememberCoroutineScope()

    var anchor by rememberState(date) {
        val initial = date ?: DateEx.Today
        LocalDate(initial.year, initial.month, 1)
    }
    var view by rememberState(initialView) { initialView }

    Box(modifier = modifier.size(gridWidth, totalHeight)) {
        key(anchor, view) {
            val pager = rememberPagerState(initialPage = DatePickerCenterPage) { Int.MAX_VALUE }
            val offset = pager.currentPage - DatePickerCenterPage

            when (view) {
                DatePickerView.Days -> {
                    val visibleMonth = anchor.plus(offset, DateTimeUnit.MONTH)

                    Column {
                        Row(
                            modifier = Modifier.size(gridWidth, cellSize),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                icon = Icons.KeyboardArrowLeft,
                                modifier = Modifier.size(textHeight),
                                tip = "上一年",
                                onClick = { scope.launch { pager.animateScrollToPage((pager.currentPage - 12).coerceAtLeast(0)) } }
                            )

                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SimpleClipText(
                                    text = "${visibleMonth.year}年",
                                    style = style,
                                    modifier = Modifier.clickable {
                                        anchor = visibleMonth
                                        view = DatePickerView.Years
                                    }
                                )
                                SimpleClipText(
                                    text = "${visibleMonth.month.number}月",
                                    style = style,
                                    modifier = Modifier.clickable {
                                        anchor = visibleMonth
                                        view = DatePickerView.Months
                                    }
                                )
                            }

                            Icon(
                                icon = Icons.KeyboardArrowRight,
                                modifier = Modifier.size(textHeight),
                                tip = "下一年",
                                onClick = { scope.launch { pager.animateScrollToPage((pager.currentPage + 12).coerceAtMost(pager.pageCount - 1)) } }
                            )
                        }

                        DatePickerWeekRow(gridWidth, cellSize, style, measurer)

                        HorizontalScrollContainer(state = pager, modifier = Modifier.size(gridWidth, cellSize * 6)) {
                            HorizontalPager(state = pager, beyondViewportPageCount = 1, key = { it }) { page ->
                                val month = anchor.plus(page - DatePickerCenterPage, DateTimeUnit.MONTH)
                                DatePickerDays(month, date, gridWidth, cellSize, style, measurer, onDateChanged)
                            }
                        }
                    }
                }

                DatePickerView.Years -> {
                    val firstYear = anchor.year - ((anchor.year % 12 + 12) % 12)
                    HorizontalScrollContainer(state = pager, modifier = Modifier.size(gridWidth, totalHeight)) {
                        HorizontalPager(state = pager, beyondViewportPageCount = 1, key = { it }) { page ->
                            val year = firstYear + (page - DatePickerCenterPage) * 12
                            DatePickerChoiceGrid(
                                width = gridWidth,
                                height = totalHeight,
                                style = style,
                                measurer = measurer,
                                labels = List(12) { (year + it).toString() },
                                selectedIndex = anchor.year - year,
                                onChoice = {
                                    anchor = LocalDate(year + it, anchor.month, 1)
                                    view = DatePickerView.Days
                                }
                            )
                        }
                    }
                }

                DatePickerView.Months -> {
                    HorizontalScrollContainer(state = pager, modifier = Modifier.size(gridWidth, totalHeight)) {
                        HorizontalPager(state = pager, beyondViewportPageCount = 1, key = { it }) { page ->
                            val year = anchor.year + page - DatePickerCenterPage
                            DatePickerChoiceGrid(
                                width = gridWidth,
                                height = totalHeight,
                                style = style,
                                measurer = measurer,
                                labels = List(12) { "${it + 1}月" },
                                selectedIndex = if (year == anchor.year) anchor.month.number - 1 else -1,
                                onChoice = {
                                    anchor = LocalDate(year, it + 1, 1)
                                    view = DatePickerView.Days
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}