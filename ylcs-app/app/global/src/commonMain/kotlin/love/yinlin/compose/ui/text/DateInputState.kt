package love.yinlin.compose.ui.text

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.datetime.LocalDate
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.Flyout
import love.yinlin.compose.ui.floating.FlyoutPosition
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.DatePicker
import love.yinlin.compose.ui.input.DatePickerState
import love.yinlin.extension.DateEx

@Stable
class DateInputState(initValue: LocalDate? = null) {
    private var currentValue by mutableStateOf(initValue)
    internal val inputState = InputState(initText = initValue?.let(DateEx.Formatter.standardDate::format).orEmpty())

    private var visible by mutableStateOf(false)
    private var selection: Pair<LocalDate?, DatePickerState>? by mutableStateOf(null)

    var value: LocalDate?
        get() = currentValue
        set(newValue) {
            if (currentValue != newValue) {
                inputState.text = newValue?.let(DateEx.Formatter.standardDate::format).orEmpty()
                currentValue = newValue
            }
        }

    internal val trailing: InputDecoration = object : InputDecoration {
        override val alignment = Alignment.CenterVertically

        @Composable
        override fun Content(state: InputState) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (value != null) {
                    Icon(icon = Icons.Clear, onClick = {
                        visible = false
                        value = null
                    })
                }
                CalendarButton()
            }
        }
    }

    @Composable
    private fun CalendarButton() {
        Flyout(
            visible = visible,
            onClickOutside = ::dismissPicker,
            position = FlyoutPosition.Bottom,
            clip = true,
            flyout = {
                selection?.let { [initialDate, picker] ->
                    LaunchedEffect(picker.date) {
                        if (picker.date != initialDate) dismissPicker()
                    }

                    Surface(
                        contentPadding = Theme.padding.value7,
                        shape = Theme.shape.v1,
                        shadowElevation = Theme.shadow.v3,
                        border = BorderStroke(Theme.border.v7, Theme.color.outline)
                    ) {
                        DatePicker(state = picker)
                    }
                }
            }
        ) {
            Icon(icon = Icons.Calendar, onClick = {
                val date = value
                selection = date to DatePickerState(date)
                visible = true
            })
        }
    }

    private fun dismissPicker() {
        if (!visible) return
        visible = false
        selection?.let { [initialDate, picker] ->
            val selectedDate = picker.date
            if (selectedDate != initialDate) value = selectedDate
        }
    }

    internal fun resetPicker() {
        visible = false
        selection = null
    }
}
