package love.yinlin.ui.component.container

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.datetime.*
import love.yinlin.common.Resource
import love.yinlin.compose.*
import love.yinlin.extension.DateEx
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.layout.ActionScope

private val lunarFestivalTable = mapOf(
    101 to "春节", 115 to "元宵", 202 to "龙抬头", 505 to "端午",
    707 to "七夕", 715 to "中元", 815 to "中秋", 909 to "重阳",
    1001 to "寒衣", 1208 to "腊八"
)

private val solarFestivalTable = mapOf(
    101 to "元旦", 214 to "情人节", 308 to "妇女节", 401 to "愚人节",
    501 to "劳动节", 601 to "儿童节", 701 to "建党节", 801 to "建军节",
    910 to "教师节", 1001 to "国庆节", 1101 to "万圣节", 1225 to "圣诞节"
)

private val lunarDayTable = arrayOf(
    "", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"
)
private val lunarDaysTable = arrayOf(
    "初", "十", "廿", "三"
)
private val lunarMonthTable = arrayOf(
    "", "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"
)
private val solarTermTable = arrayOf(
    "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
    "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
    "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
    "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
)

private val LocalDate.lunar: String get() = Resource.lunar?.let { table ->
    val solarYear = this.year
    val solarMonth = this.month.number
    val solarDay = this.day
    val index = ((solarYear - 2000) * 12 * 31 + (solarMonth - 1) * 31 + (solarDay - 1)) * 2
    val byte0 = table[index].toInt() and 0xFF
    val byte1 = table[index + 1].toInt() and 0xFF
    val combined = (byte1 shl 8) or byte0
    val isTerm = (combined and 0x01) != 0
    val isLeap = (combined and 0x02) != 0
    // val isCurrentYear = (combined and 0x04) != 0
    val lunarMonth = ((combined ushr 3) and 0x0F) + 1
    val lunarDay = ((combined ushr 7) and 0x1F) + 1
    solarFestivalTable[solarMonth * 100 + solarDay] ?: lunarFestivalTable[lunarMonth * 100 + lunarDay] ?:
    if (isTerm) solarTermTable[(solarMonth - 1) * 2 + solarDay / 16]
    else when (lunarDay) {
        1 -> "${if (isLeap) "闰" else ""}${lunarMonthTable[lunarMonth]}月"
        10 -> "初十"
        20 -> "二十"
        30 -> "三十"
        else -> "${lunarDaysTable[lunarDay / 10]}${lunarDayTable[lunarDay % 10]}"
    }
} ?: ""

@Stable
class CalendarState : PagerState(currentPage = 5) { override val pageCount: Int = 12 }

private fun indexShadowDate(index: Int): LocalDate {
    val today = DateEx.Today
    val value = index - 5 + today.month.number
    return if (value <= 0) LocalDate(year = today.year - 1, month = value + 12, day = 1)
    else if (value > 12) LocalDate(year = today.year + 1, month = value - 12, day = 1)
    else LocalDate(year = today.year, month = value, day = 1)
}

@Composable
private fun CalendarHeader(
    state: CalendarState,
    modifier: Modifier = Modifier,
    actions: @Composable ActionScope.() -> Unit = { }
) {
    val currentDate by rememberDerivedState { indexShadowDate(state.settledPage) }

    Row(
        modifier = modifier.padding(start = CustomTheme.padding.horizontalExtraSpace, end = CustomTheme.padding.horizontalSpace),
        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = remember(currentDate) { "${currentDate.year}年${currentDate.month.number}月" },
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            ActionScope.Right.actions()
        }
    }
}

@Composable
private fun CalendarWeekGrid(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        "一二三四五六日".forEach {
            Text(
                text = it.toString(),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CalendarDayGrid(
    state: CalendarState,
    events: Map<LocalDate, String>,
    modifier: Modifier = Modifier,
    onEventClick: (LocalDate) -> Unit
) {
    HorizontalPager(
        state = state,
        beyondViewportPageCount = 2,
        key = { it },
        modifier = modifier
    ) { pageIndex ->
        val currentDate = remember(pageIndex) { indexShadowDate(pageIndex) }
        val startDay = remember(currentDate) { currentDate.dayOfWeek.isoDayNumber - 1 }
        val endDay = remember(currentDate, startDay) {
            val tmp = currentDate.plus(1, DateTimeUnit.MONTH)
            val endOfMonth = LocalDate(tmp.year, tmp.month, 1).minus(1, DateTimeUnit.DAY)
            startDay + endOfMonth.day - 1
        }
        val startDate = remember(currentDate, startDay) { currentDate.minus(startDay, DateTimeUnit.DAY) }
        val today = remember(currentDate) { DateEx.Today }

        Column(modifier = Modifier.fillMaxWidth()) {
            repeat(6) { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val dayIndex = row * 7 + col
                        val date = remember(startDate, dayIndex) { startDate.plus(dayIndex, DateTimeUnit.DAY) }
                        val eventTitle = events[date]
                        val color = when {
                            date == today -> MaterialTheme.colorScheme.onPrimaryContainer
                            eventTitle != null -> MaterialTheme.colorScheme.primary
                            dayIndex !in startDay..endDay -> MaterialTheme.colorScheme.onSurfaceVariant
                            date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY -> MaterialTheme.colorScheme.tertiary
                            else -> Colors.Unspecified
                        }
                        val text = remember(eventTitle, date) { eventTitle ?: date.lunar }

                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)
                            .condition(eventTitle != null) { clickable(onClick = { onEventClick(date) }) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (date == today) {
                                Box(modifier = Modifier.matchParentSize().background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = date.day.toString(),
                                    color = color,
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                                Text(
                                    text = text,
                                    color = color,
                                    style = CustomTheme.typography.bodyExtraSmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
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
    state: CalendarState = remember { CalendarState() },
    events: Map<LocalDate, String> = remember { emptyMap() },
    modifier: Modifier = Modifier,
    actions: @Composable ActionScope.() -> Unit = {},
    onEventClick: (LocalDate) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
    ) {
        CalendarHeader(
            state = state,
            modifier = Modifier.fillMaxWidth().padding(vertical = CustomTheme.padding.verticalExtraSpace),
            actions = actions
        )
        CalendarWeekGrid(
            modifier = Modifier.fillMaxWidth()
        )
        CalendarDayGrid(
            state = state,
            events = events,
            modifier = Modifier.fillMaxWidth(),
            onEventClick = onEventClick
        )
    }
}