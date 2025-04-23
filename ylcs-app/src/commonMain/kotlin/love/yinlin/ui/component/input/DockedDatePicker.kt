package love.yinlin.ui.component.input

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import love.yinlin.extension.*
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.layout.ExpandableLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockedDatePicker(
	hint: String,
	initDate: LocalDate? = null,
	onDateSelected: (LocalDate?) -> Unit,
	modifier: Modifier = Modifier
) {
	var isShow by rememberState { false }
	val datePickerState = rememberDatePickerState(selectableDates = remember {
		val today = DateEx.Today
		val start = today.minus(6, DateTimeUnit.MONTH)
		val end = today.plus(6, DateTimeUnit.MONTH)
		object : SelectableDates {
			override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis.toLocalDate?.let {
				it >= start && it <= end
			} ?: false
			override fun isSelectableYear(year: Int): Boolean = year >= start.year && year <= end.year
		}
	})
	val text by rememberDerivedState {
		datePickerState.selectedDateMillis?.toLocalDate?.let {
			DateEx.Formatter.standardDate.format(it)
		} ?: "未选择"
	}

	LaunchedEffect(initDate) {
		datePickerState.selectedDateMillis = initDate?.toLong
	}

	LaunchedEffect(datePickerState.selectedDateMillis) {
		isShow = false
		onDateSelected(datePickerState.selectedDateMillis?.let {
			Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
		})
	}

	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(5.dp)
	) {
		OutlinedTextField(
			value = text,
			onValueChange = { },
			label = {
				Text(
					text = hint,
					style = MaterialTheme.typography.titleMedium
				)
			},
			readOnly = true,
			trailingIcon = {
				Row(
					modifier = Modifier.padding(end = 10.dp),
					horizontalArrangement = Arrangement.spacedBy(5.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					ClickIcon(
						icon = Icons.Outlined.DateRange,
						onClick = { isShow = !isShow }
					)
					ClickIcon(
						icon = Icons.Outlined.Close,
						onClick = { datePickerState.selectedDateMillis = null }
					)
				}
			},
			modifier = Modifier.fillMaxWidth()
		)
		ExpandableLayout(isExpanded = isShow) {
			DatePicker(
				state = datePickerState,
				modifier = Modifier.fillMaxWidth(),
				title = null,
				headline = null,
				showModeToggle = false,
				colors = DatePickerDefaults.colors().copy(
					containerColor = MaterialTheme.colorScheme.surface,
					titleContentColor = MaterialTheme.colorScheme.onSurface,
					headlineContentColor = MaterialTheme.colorScheme.onSurface,
					weekdayContentColor = MaterialTheme.colorScheme.onSurface,
					subheadContentColor = MaterialTheme.colorScheme.onSurface,
					navigationContentColor = MaterialTheme.colorScheme.onSurface,
					yearContentColor = MaterialTheme.colorScheme.onSurface,
					currentYearContentColor = MaterialTheme.colorScheme.onSurface,
					disabledYearContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
					selectedYearContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
					disabledSelectedYearContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
					selectedYearContainerColor = MaterialTheme.colorScheme.secondaryContainer,
					dayContentColor = MaterialTheme.colorScheme.onSurface,
					selectedDayContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
					disabledDayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
					selectedDayContainerColor = MaterialTheme.colorScheme.secondaryContainer,
					todayContentColor = MaterialTheme.colorScheme.onSurface,
					todayDateBorderColor = MaterialTheme.colorScheme.secondary
				)
			)
		}
	}
}