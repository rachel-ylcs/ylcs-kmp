package love.yinlin.ui.component.container

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastForEachIndexed
import love.yinlin.compose.*
import love.yinlin.ui.component.image.MiniIcon
import kotlin.jvm.JvmName

@Composable
private fun <T> TabBar(
    currentPage: Int,
    onNavigate: (Int) -> Unit,
    onLongClick: ((Int) -> Unit)? = null,
    items: List<T>,
    modifier: Modifier = Modifier,
    content: @Composable (Boolean, T) -> Unit,
) {
    PrimaryScrollableTabRow(
        modifier = modifier,
        selectedTabIndex = currentPage,
        edgePadding = CustomTheme.padding.zeroSpace,
        indicator = {
            if (currentPage < items.size) {
                TabRowDefaults.PrimaryIndicator(
                    Modifier.tabIndicatorOffset(currentPage, matchContentSize = false),
                    width = Dp.Unspecified,
                    height = CustomTheme.size.little
                )
            }
        },
        divider = {}
    ) {
        items.fastForEachIndexed { index, item ->
            val isSelected = currentPage == index
            Box(
                modifier = Modifier.combinedClickable(
                    onClick = {
                        if (!isSelected) onNavigate(index)
                    },
                    onLongClick = {
                        if (currentPage == index) onLongClick?.invoke(index)
                    }
                ).padding(CustomTheme.padding.equalSpace),
                contentAlignment = Alignment.Center
            ) {
                content(isSelected, item)
            }
        }
    }
}

@JvmName("TabBarWithIcon")
@Composable
fun TabBar(
    currentPage: Int,
    onNavigate: (Int) -> Unit,
    onLongClick: ((Int) -> Unit)? = null,
    items: List<Pair<String, ImageVector>>,
    modifier: Modifier = Modifier
) {
    TabBar(
        currentPage = currentPage,
        onNavigate = onNavigate,
        onLongClick = onLongClick,
        items = items,
        modifier = modifier
    ) { isSelected, (title, icon) ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniIcon(
                icon = icon,
                size = CustomTheme.size.microIcon,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = if (isSelected) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TabBar(
    currentPage: Int,
    onNavigate: (Int) -> Unit,
    onLongClick: ((Int) -> Unit)? = null,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    TabBar(
        currentPage = currentPage,
        onNavigate = onNavigate,
        onLongClick = onLongClick,
        items = items,
        modifier = modifier
    ) { isSelected, title ->
        Text(
            text = title,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            style = if (isSelected) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}