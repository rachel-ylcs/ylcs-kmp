package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import love.yinlin.platform.Platform

@Composable
fun <T> PaginationStaggeredGrid(
    items: List<T>,
    key: ((T) -> Any)? = null,
    columns: StaggeredGridCells,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    canRefresh: Boolean = true,
    canLoading: Boolean = false,
    onRefresh: (suspend () -> Unit)? = null,
    onLoading: (suspend () -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
    verticalItemSpacing: Dp = 0.dp,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(0.dp),
    header: (@Composable LazyStaggeredGridItemScope.() -> Unit)? = null,
    itemContent: @Composable LazyStaggeredGridItemScope.(T) -> Unit
) {
    if (Platform.contains(*Platform.Phone)) {
        SwipePaginationStaggeredGrid(
            items = items,
            key = key,
            columns = columns,
            state = state,
            canRefresh = canRefresh,
            canLoading = canLoading,
            onRefresh = onRefresh,
            onLoading = onLoading,
            modifier = modifier,
            contentPadding = contentPadding,
            verticalItemSpacing = verticalItemSpacing,
            horizontalArrangement = horizontalArrangement,
            header = header,
            itemContent = itemContent
        )
    }
    else {
        ClickPaginationStaggeredGrid(
            items = items,
            key = key,
            columns = columns,
            state = state,
            canLoading = canLoading,
            onLoading = onLoading,
            modifier = modifier,
            contentPadding = contentPadding,
            verticalItemSpacing = verticalItemSpacing,
            horizontalArrangement = horizontalArrangement,
            header = header,
            itemContent = itemContent
        )
    }
}