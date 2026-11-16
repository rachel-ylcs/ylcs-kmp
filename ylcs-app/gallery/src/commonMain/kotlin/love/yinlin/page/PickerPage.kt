package love.yinlin.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Stable
import love.yinlin.GalleryCaption
import love.yinlin.Page
import love.yinlin.PageGroup
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.ColorPicker
import love.yinlin.compose.ui.input.DatePicker
import love.yinlin.compose.ui.input.DatePickerState
import love.yinlin.compose.ui.input.TimePicker
import love.yinlin.compose.ui.input.TimePickerState

@Stable
object PickerPage : Page(
    id = "picker",
    icon = Icons.Calendar,
    title = "选择器",
    english = "Picker",
    description = "通过颜色、日期和时间选择器确定具体值。",
    keywords = "颜色选择器 日期选择器 时间选择器 ColorPicker DatePicker TimePicker",
    group = PageGroup.Interaction,
) {
    private val demoColorPicker = component("ColorPicker", "选择颜色并调整透明度。", "取色 透明度")
    private val demoDatePicker = component("DatePicker", "按年、月浏览日历，点击选定日期。", "日期选择器 DatePickerState")
    private val demoTimePicker = component("TimePicker", "滚动时分秒三列来选择时间。", "时间选择器 TimePickerState")

    @Composable
    override fun Content() {
        ComponentColumn {
            Component(demoColorPicker) {
                ColorPicker(onColorChangeFinished = {})
            }

            Component(demoDatePicker) {
                val state by rememberState { DatePickerState() }

                DatePicker(state)
                GalleryCaption(state.date?.toString() ?: "尚未选择日期")
            }

            Component(demoTimePicker) {
                val state by rememberState { TimePickerState() }

                TimePicker(state)
                GalleryCaption(state.time.toString())
            }
        }
    }
}
