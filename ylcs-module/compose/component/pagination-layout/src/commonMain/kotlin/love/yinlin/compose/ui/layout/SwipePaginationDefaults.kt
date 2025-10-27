package love.yinlin.compose.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.ui.animation.LoadingAnimation
import love.yinlin.compose.ui.image.MiniIcon

@Composable
internal fun DefaultSwipePaginationHeader(
    status: PaginationStatus,
    progress: Float,
    runningLabel: String = "刷新中...",
    pullLabel: String = "继续下拉刷新",
    releaseLabel: String = "释放立即刷新"
) {
    Row(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = progress)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace, Alignment.CenterHorizontally)
    ) {
        if (status == PaginationStatus.RUNNING) LoadingAnimation(
            size = CustomTheme.size.mediumIcon,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        else MiniIcon(
            icon = Icons.Outlined.ArrowDownward,
            size = CustomTheme.size.mediumIcon,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = when (status) {
                PaginationStatus.RUNNING -> runningLabel
                PaginationStatus.PULL -> pullLabel
                PaginationStatus.RELEASE -> releaseLabel
                else -> ""
            },
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
internal fun DefaultSwipePaginationFooter(
    status: PaginationStatus,
    progress: Float,
    runningLabel: String = "加载中...",
    pullLabel: String = "上拉加载更多",
    releaseLabel: String = "释放立即加载"
) {
    Row(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = progress)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace, Alignment.CenterHorizontally)
    ) {
        if (status == PaginationStatus.RUNNING) LoadingAnimation(
            size = CustomTheme.size.mediumIcon,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        else MiniIcon(
            icon = Icons.Outlined.ArrowUpward,
            size = CustomTheme.size.mediumIcon,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = when (status) {
                PaginationStatus.RUNNING -> runningLabel
                PaginationStatus.PULL -> pullLabel
                PaginationStatus.RELEASE -> releaseLabel
                else -> ""
            },
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}