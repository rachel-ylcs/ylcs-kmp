package love.yinlin.ui.component.extra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import love.yinlin.ThemeColor
import love.yinlin.extension.rememberDerivedState
import love.yinlin.ui.component.image.ClickIcon

@Stable
private data class DayItem(
	val date: LocalDate,
	val title: String,
	val type: Type
) {
	enum class Type {
		NORMAL, OTHER_MONTH, WEEKEND, TODAY, EVENT
	}
}

@Stable
class CalendarState {
	internal val pagerState = object : PagerState(currentPage = 5) { override val pageCount: Int = 12 }
	val events = mutableStateMapOf<LocalDate, String>()
}

private val Today: LocalDate get() = Clock.System.now().toLocalDateTime(TimeZone.UTC).date

private fun indexShadowDate(index: Int): LocalDate {
	val today = Today
	val value = index - 5 + today.monthNumber
	return if (value <= 0) LocalDate(year = today.year - 1, monthNumber = value + 12, dayOfMonth = 1)
	else if (value > 12) LocalDate(year = today.year + 1, monthNumber = value - 12, dayOfMonth = 1)
	else LocalDate(year = today.year, monthNumber = value, dayOfMonth = 1)
}

@Composable
private fun CalendarHeader(
	currentDate: LocalDate,
	modifier: Modifier = Modifier,
	onRefresh: () -> Unit
) {
	Row(
		modifier = modifier.padding(horizontal = 10.dp),
		horizontalArrangement = Arrangement.spacedBy(20.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = "${currentDate.year}年${currentDate.monthNumber}月",
			style = MaterialTheme.typography.titleLarge,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.weight(1f)
		)
		ClickIcon(
			imageVector = Icons.Default.Refresh,
			onClick = onRefresh
		)
	}
}

@Composable
private fun CalendarWeekGrid(
	modifier: Modifier = Modifier
) {
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
	modifier: Modifier = Modifier
) {
	HorizontalPager(
		state = state.pagerState,
		beyondViewportPageCount = 1,
		modifier = modifier
	) { pageIndex ->
		val currentDate = remember { indexShadowDate(pageIndex) }
		val startDay = currentDate.dayOfWeek.isoDayNumber - 1
		val endDay = remember(currentDate) {
			val tmp = currentDate.plus(1, DateTimeUnit.MONTH)
			val endOfMonth = LocalDate(tmp.year, tmp.month, 1).minus(1, DateTimeUnit.DAY)
			startDay + endOfMonth.dayOfMonth - 1
		}
		val startDate = remember(currentDate, startDay) { currentDate.minus(startDay, DateTimeUnit.DAY) }
		val today = remember(currentDate) { Today }

		FlowRow(
			maxItemsInEachRow = 7,
			verticalArrangement = Arrangement.spacedBy(10.dp),
			modifier = Modifier.fillMaxWidth()
		) {
			repeat(42) { dayIndex ->
				val date = startDate.plus(dayIndex, DateTimeUnit.DAY)
				Column(
					modifier = Modifier.weight(1f),
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					val color = when {
						state.events.contains(date) -> MaterialTheme.colorScheme.primary
						date == today -> MaterialTheme.colorScheme.secondary
						dayIndex !in startDay..endDay -> ThemeColor.fade
						date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY -> MaterialTheme.colorScheme.tertiary
						else -> LocalTextStyle.current.color
					}

					Text(
						text = date.dayOfMonth.toString(),
						color = color,
						style = MaterialTheme.typography.labelLarge,
						textAlign = TextAlign.Center,
						maxLines = 1,
						overflow = TextOverflow.Clip,
						modifier = Modifier.fillMaxWidth()
					)
					Text(
						text = "惊蛰",
						color = color,
						style = MaterialTheme.typography.bodyMedium,
						textAlign = TextAlign.Center,
						maxLines = 1,
						overflow = TextOverflow.Clip,
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}
	}
}

@Composable
fun Calendar(
	state: CalendarState = remember { CalendarState() },
	modifier: Modifier = Modifier,
	onRefresh: () -> Unit = {}
) {
	val currentDate by rememberDerivedState { indexShadowDate(state.pagerState.currentPage) }

	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(10.dp)
	) {
		CalendarHeader(
			currentDate = currentDate,
			modifier = Modifier.fillMaxWidth(),
			onRefresh = onRefresh
		)
		CalendarWeekGrid(modifier = Modifier.fillMaxWidth())
		CalendarDayGrid(
			state = state,
			modifier = Modifier.fillMaxWidth()
		)
	}
}