package love.yinlin.compose.ui.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.animation.LoadingAnimation
import love.yinlin.compose.ui.icon.M3Icons
import love.yinlin.compose.ui.image.MiniIcon

@Composable
internal fun DefaultClickPaginationIndicator(
    status: PaginationStatus,
    onLoading: () -> Unit,
    runningLabel: String = "加载中...",
    commonLabel: String = "加载更多"
) {
    Box(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onLoading),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(CustomTheme.padding.equalValue),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace, Alignment.CenterHorizontally)
        ) {
            if (status == PaginationStatus.RUNNING) LoadingAnimation(size = CustomTheme.size.mediumIcon)
            else MiniIcon(icon = M3Icons.History, size = CustomTheme.size.mediumIcon)
            Text(text = if (status == PaginationStatus.RUNNING) runningLabel else commonLabel)
        }
    }
}