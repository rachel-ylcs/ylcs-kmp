package love.yinlin.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.Page
import love.yinlin.compose.ui.widget.Calendar

@Stable
object WidgetPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            Component("Calendar") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Calendar(modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth())
                }
            }
        }
    }
}
