package love.yinlin.compose.ui.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter

@Composable
internal fun DefaultClickPaginationIndicator(
    status: PaginationStatus,
    onLoading: () -> Unit,
    runningLabel: String = "加载中...",
    commonLabel: String = "加载更多"
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onLoading).padding(Theme.padding.eValue),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9, Alignment.CenterHorizontally)
    ) {
        TextIconAdapter { idIcon, idText ->
            if (status == PaginationStatus.RUNNING) CircleLoading.Content(modifier = Modifier.idIcon())
            else Icon(icon = Icons.History, modifier = Modifier.idIcon())
            Text(text = if (status == PaginationStatus.RUNNING) runningLabel else commonLabel, modifier = Modifier.idText())
        }
    }
}