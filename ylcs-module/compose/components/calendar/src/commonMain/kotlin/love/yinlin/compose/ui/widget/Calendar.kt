package love.yinlin.compose.ui.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.localComposition
import love.yinlin.compose.scaleSize
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.HorizontalScrollContainer
import love.yinlin.compose.ui.node.keepSize
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.measureTextHeight
import love.yinlin.extension.DateEx

@Composable
fun rememberCalendarState() = remember { CalendarState() }

private val LocalCalendarDayTextStyle = localComposition<TextStyle>()
private val LocalCalendarDayNumberStyle = localComposition<TextStyle>()
private val LocalCalendarCellPaddingRatio = localComposition<Float>()
private val LocalCalendarCellSize = localComposition<Dp>()

@Composable
private fun rememberCalendarSize(): Triple<Dp, Dp, Dp> {
    val cellSize = LocalCalendarCellSize.current
    val cellPadding = cellSize * LocalCalendarCellPaddingRatio.current
    val totalWidth = cellSize * 7 + cellPadding * 14
    return Triple(cellSize, cellPadding, totalWidth)
}

@Composable
private fun CalendarHeader(
    settledPage: Int,
    actions: @Composable RowScope.() -> Unit = { }
) {
    val currentDateText = remember(settledPage) {
        val date = CalendarState.indexShadowDate(settledPage)
        "${date.year}年${date.month.number}月"
    }

    val dayTextStyle = LocalCalendarDayTextStyle.current
    val headerStyle = dayTextStyle.scaleSize(1.6f, true)

    val [_, _, totalWidth] = rememberCalendarSize()

    Row(
        modifier = Modifier.width(totalWidth),
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h10),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SimpleEllipsisText(
            text = currentDateText,
            modifier = Modifier.weight(1f).padding(start = Theme.padding.h / 2),
            style = headerStyle,
        )
        ActionScope.Right.Container(content = actions)
    }
}

@Composable
private fun CalendarWeekGrid() {
    val dayTextStyle = LocalCalendarDayTextStyle.current
    val headerStyle = dayTextStyle.scaleSize(1.4f, true)

    val [_, cellPadding, totalWidth] = rememberCalendarSize()

    Row(modifier = Modifier.width(totalWidth)) {
        "一二三四五六日".forEach {
            SimpleClipText(
                text = it.toString(),
                modifier = Modifier.weight(1f).padding(horizontal = cellPadding),
                style = headerStyle,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CalendarDayGrid(
    state: CalendarState,
    events: Map<Long, String>,
    onEventClick: (LocalDate) -> Unit,
) {
    val [cellSize, cellPadding, totalWidth] = rememberCalendarSize()

    HorizontalScrollContainer(state = state, modifier = Modifier.keepSize().size(totalWidth, cellSize * 6 + cellPadding * 12)) {
        HorizontalPager(
            state = state,
            beyondViewportPageCount = 2,
            key = { it },
        ) { pageIndex ->
            val currentDate = remember(pageIndex) { CalendarState.indexShadowDate(pageIndex) }
            val startDay = currentDate.dayOfWeek.isoDayNumber - 1
            val endDay = run {
                val tmp = currentDate.plus(1, DateTimeUnit.MONTH)
                val endOfMonth = LocalDate(tmp.year, tmp.month, 1).minus(1, DateTimeUnit.DAY)
                startDay + endOfMonth.day - 1
            }
            val startDate = currentDate.minus(startDay, DateTimeUnit.DAY)
            val today = DateEx.Today

            Column {
                repeat(6) { i ->
                    Row {
                        repeat(7) { j ->
                            val dayIndex = 7 * i + j
                            val date = startDate.plus(dayIndex, DateTimeUnit.DAY)
                            val dateDays = date.toEpochDays()

                            key(dateDays) {
                                val isToday = date == today
                                val eventTitle = events[dateDays]
                                val color = when {
                                    isToday -> Theme.color.onContainer
                                    eventTitle != null -> Theme.color.primary
                                    dayIndex !in startDay..endDay -> LocalColorVariant.current.copy(alpha = 0.5f)
                                    date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY -> Theme.color.tertiary
                                    else -> LocalColor.current
                                }
                                val todayBackgroundColor = Theme.color.primaryContainer

                                val text = remember(eventTitle, date, LunarLoader.table) { eventTitle ?: LunarLoader.lunar(date) }

                                Column(
                                    modifier = Modifier.weight(1f).aspectRatio(1f).clip(Theme.shape.v7).clickable {
                                        if (eventTitle != null) onEventClick(date)
                                    }.drawWithContent {
                                        if (isToday) drawRect(todayBackgroundColor)
                                        drawContent()
                                    },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val fontSize = Theme.typography.v8.fontSize / when (text.length) {
                                        in 0 .. 2 -> 1f
                                        in 3 .. 4 -> 1.25f
                                        else -> 1.5f
                                    }

                                    SimpleClipText(text = date.day.toString(), color = color, style = LocalCalendarDayNumberStyle.current)
                                    SimpleClipText(text = text, color = color, style = LocalCalendarDayTextStyle.current.copy(fontSize = fontSize))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Calendar(
    state: CalendarState = rememberCalendarState(),
    modifier: Modifier = Modifier,
    events: Map<Long, String> = emptyMap(),
    actions: @Composable RowScope.() -> Unit = {},
    onEventClick: (LocalDate) -> Unit = {},
    style: TextStyle = Theme.typography.v8,
    cellPaddingRatio: Float = 0.1f,
) {
    LunarLoader.Load()

    val dayNumberStyle = style.scaleSize(1.6f, true)

    val cellSize = measureTextHeight("31", dayNumberStyle, "清明", style) { v1, v2 -> v1 + v2 }

    CompositionLocalProvider(
        LocalCalendarDayTextStyle provides style,
        LocalCalendarDayNumberStyle provides dayNumberStyle,
        LocalCalendarCellPaddingRatio provides cellPaddingRatio,
        LocalCalendarCellSize provides cellSize
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(cellSize * 0.3f),
        ) {
            CalendarHeader(settledPage = state.settledPage, actions = actions)
            CalendarWeekGrid()
            CalendarDayGrid(state = state, events = events, onEventClick = onEventClick)
        }
    }
}