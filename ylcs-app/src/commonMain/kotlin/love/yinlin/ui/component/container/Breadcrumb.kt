package love.yinlin.ui.component.container

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.common.ThemeValue
import love.yinlin.ui.component.image.MiniIcon

@Composable
private fun BreadcrumbItem(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clickable(onClick = onClick)
            .padding(ThemeValue.Padding.Value)
    )
}

@Composable
fun <T> Breadcrumb(
    items: List<T>,
    key: ((Int, T) -> Any)? = null,
    modifier: Modifier = Modifier,
    onClick: (Int) -> Unit
) {
    val state = rememberLazyListState()

    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) state.animateScrollToItem(items.size - 1)
    }

    LazyRow(
        modifier = modifier,
        state = state,
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(
            items = items,
            key = key
        ) { index, item ->
            val lastItem = index == items.size - 1
            BreadcrumbItem(
                text = item.toString(),
                color = if (lastItem) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                onClick = { onClick(index) }
            )
            if (!lastItem) MiniIcon(icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight)
        }
    }
}