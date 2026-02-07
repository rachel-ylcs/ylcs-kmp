package love.yinlin.compose.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.LocalColor
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter

@Composable
internal fun DefaultSwipeLayout(
    status: PaginationStatus,
    progress: Float,
    icon: ImageVector,
    runningLabel: String,
    pullLabel: String,
    releaseLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxSize().background(Theme.color.primaryContainer.copy(alpha = progress)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9, Alignment.CenterHorizontally)
    ) {
        CompositionLocalProvider(LocalColor provides Theme.color.onContainer) {
            TextIconAdapter { idIcon, idText ->
                if (status == PaginationStatus.RUNNING) CircleLoading.Content(modifier = Modifier.idIcon())
                else Icon(icon = icon, modifier = Modifier.idIcon())
                Text(text = when (status) {
                    PaginationStatus.RUNNING -> runningLabel
                    PaginationStatus.PULL -> pullLabel
                    PaginationStatus.RELEASE -> releaseLabel
                    else -> ""
                }, modifier = Modifier.idText())
            }
        }
    }
}

@Composable
@NonRestartableComposable
internal fun DefaultSwipePaginationHeader(
    status: PaginationStatus,
    progress: Float,
    runningLabel: String = "刷新中...",
    pullLabel: String = "继续下拉刷新",
    releaseLabel: String = "释放立即刷新"
) {
    DefaultSwipeLayout(status, progress, Icons.ArrowDownward, runningLabel, pullLabel, releaseLabel)
}

@Composable
@NonRestartableComposable
internal fun DefaultSwipePaginationFooter(
    status: PaginationStatus,
    progress: Float,
    runningLabel: String = "加载中...",
    pullLabel: String = "上拉加载更多",
    releaseLabel: String = "释放立即加载"
) {
    DefaultSwipeLayout(status, progress, Icons.ArrowUpward, runningLabel, pullLabel, releaseLabel)
}