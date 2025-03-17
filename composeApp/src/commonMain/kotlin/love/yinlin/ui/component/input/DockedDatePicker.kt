package love.yinlin.ui.component.input

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import love.yinlin.extension.DateEx
import love.yinlin.extension.rememberDerivedState
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.layout.ExpandableLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockedDatePicker(
	hint: String,
	onDateSelected: (LocalDate?) -> Unit,
	modifier: Modifier = Modifier
) {
	var isShow by remember { mutableStateOf(false) }
	val datePickerState = rememberDatePickerState()
	val text by rememberDerivedState {
		datePickerState.selectedDateMillis?.let {
			val date = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
			DateEx.Formatter.standardDate.format(date)
		} ?: "未选择"
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
						imageVector = Icons.Outlined.DateRange,
						onClick = { isShow = !isShow }
					)
					ClickIcon(
						imageVector = Icons.Outlined.Close,
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
				showModeToggle = false
			)
		}
	}
}