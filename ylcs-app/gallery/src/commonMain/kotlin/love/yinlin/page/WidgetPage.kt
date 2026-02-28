package love.yinlin.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.Page
import love.yinlin.compose.ui.widget.Calendar

@Stable
object WidgetPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            Component("Calendar") {
                Calendar()
            }
        }
    }
}