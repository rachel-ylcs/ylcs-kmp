package love.yinlin.compose.ui.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
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
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.scaleSize
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.node.keepSize
import love.yinlin.compose.ui.resources.Res
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.measureTextHeight
import love.yinlin.concurrent.Mutex
import love.yinlin.extension.DateEx

@Stable
private val lunarFestivalTable = mapOf(
    101 to "春节", 115 to "元宵", 202 to "龙抬头", 505 to "端午",
    707 to "七夕", 715 to "中元", 815 to "中秋", 909 to "重阳",
    1001 to "寒衣", 1208 to "腊八"
)

@Stable
private val solarFestivalTable = mapOf(
    101 to "元旦", 214 to "情人节", 308 to "妇女节", 401 to "愚人节",
    501 to "劳动节", 601 to "儿童节", 701 to "建党节", 801 to "建军节",
    910 to "教师节", 1001 to "国庆节", 1101 to "万圣节", 1225 to "圣诞节"
)

@Stable
private val lunarDayTable = arrayOf(
    "", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"
)

@Stable
private val lunarDaysTable = arrayOf(
    "初", "十", "廿", "三"
)

@Stable
private val lunarMonthTable = arrayOf(
    "", "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"
)

@Stable
private val solarTermTable = arrayOf(
    "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
    "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
    "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
    "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
)

@Stable
private object LunarLoader {
    var table: ByteArray? by mutableRefStateOf(null)
        private set

    private val mutex = Mutex()

    @Composable
    fun Load() {
        LaunchedEffect(Unit) {
            if (table == null) {
                mutex.with {
                    if (table == null) table = Res.readBytes("files/lunar.bin")
                }
            }
        }
    }

    fun lunar(date: LocalDate): String {
        val lunarTable = table ?: return ".."
        val solarYear = date.year
        val solarMonth = date.month.number
        val solarDay = date.day
        val index = ((solarYear - 2000) * 12 * 31 + (solarMonth - 1) * 31 + (solarDay - 1)) * 2
        val byte0 = lunarTable[index].toInt() and 0xFF
        val byte1 = lunarTable[index + 1].toInt() and 0xFF
        val combined = (byte1 shl 8) or byte0
        val isTerm = (combined and 0x01) != 0
        val isLeap = (combined and 0x02) != 0
        // val isCurrentYear = (combined and 0x04) != 0
        val lunarMonth = ((combined ushr 3) and 0x0F) + 1
        val lunarDay = ((combined ushr 7) and 0x1F) + 1

        return solarFestivalTable[solarMonth * 100 + solarDay] ?: lunarFestivalTable[lunarMonth * 100 + lunarDay] ?:
        if (isTerm) solarTermTable[(solarMonth - 1) * 2 + solarDay / 16]
        else when (lunarDay) {
            1 -> "${if (isLeap) "闰" else ""}${lunarMonthTable[lunarMonth]}月"
            10 -> "初十"
            20 -> "二十"
            30 -> "三十"
            else -> "${lunarDaysTable[lunarDay / 10]}${lunarDayTable[lunarDay % 10]}"
        }
    }
}

@Stable
class CalendarState : PagerState(currentPage = 5) {
    override val pageCount: Int = 12

    companion object {
        internal fun indexShadowDate(index: Int): LocalDate {
            val today = DateEx.Today
            val value = index - 5 + today.month.number
            return if (value <= 0) LocalDate(year = today.year - 1, month = value + 12, day = 1)
            else if (value > 12) LocalDate(year = today.year + 1, month = value - 12, day = 1)
            else LocalDate(year = today.year, month = value, day = 1)
        }
    }
}

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
    state: CalendarState,
    actions: @Composable RowScope.() -> Unit = { }
) {
    val currentDate = remember(state.settledPage) { CalendarState.indexShadowDate(state.settledPage) }

    val dayTextStyle = LocalCalendarDayTextStyle.current
    val headerStyle = dayTextStyle.scaleSize(1.6f, true)

    val (_, _, totalWidth) = rememberCalendarSize()

    Row(
        modifier = Modifier.width(totalWidth),
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h10),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SimpleEllipsisText(
            text = "${currentDate.year}年${currentDate.month.number}月",
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

    val (_, cellPadding, totalWidth) = rememberCalendarSize()

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
    val (cellSize, cellPadding, totalWidth) = rememberCalendarSize()
    val today = remember { DateEx.Today }

    HorizontalPager(
        modifier = Modifier.keepSize().size(totalWidth, cellSize * 6 + cellPadding * 12),
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

        Column {
            repeat(6) { i ->
                Row {
                    repeat(7) { j ->
                        val dayIndex = 7 * i + j

                        val date = startDate.plus(dayIndex, DateTimeUnit.DAY)
                        val isToday = date == today
                        val dateDays = date.toEpochDays()
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
                                if (isToday) drawCircle(todayBackgroundColor)
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

    val cellSize = measureTextHeight("31", dayNumberStyle, solarTermTable[0], style) { v1, v2 -> v1 + v2 }

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
            CalendarHeader(state = state, actions = actions)
            CalendarWeekGrid()
            CalendarDayGrid(state = state, events = events, onEventClick = onEventClick)
        }
    }
}