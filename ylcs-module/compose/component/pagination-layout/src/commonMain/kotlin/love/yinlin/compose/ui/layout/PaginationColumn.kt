package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.CustomTheme
import love.yinlin.platform.Platform

@Composable
fun <T> PaginationColumn(
    items: List<T>,
    key: ((T) -> Any)? = null,
    state: LazyListState = rememberLazyListState(),
    canRefresh: Boolean = true,
    canLoading: Boolean = false,
    onRefresh: (suspend () -> Unit)? = null,
    onLoading: (suspend () -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = CustomTheme.padding.zeroValue,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    header: (@Composable LazyItemScope.() -> Unit)? = null,
    itemDivider: PaddingValues? = null,
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    if (Platform.contains(*Platform.Phone)) {
        SwipePaginationColumn(
            items = items,
            key = key,
            state = state,
            canRefresh = canRefresh,
            onRefresh = onRefresh,
            canLoading = canLoading,
            onLoading = onLoading,
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            header = header,
            itemDivider = itemDivider,
            itemContent = itemContent
        )
    }
    else {
        ClickPaginationColumn(
            items = items,
            key = key,
            state = state,
            canLoading = canLoading,
            onLoading = onLoading,
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            header = header,
            itemDivider = itemDivider,
            itemContent = itemContent
        )
    }
}