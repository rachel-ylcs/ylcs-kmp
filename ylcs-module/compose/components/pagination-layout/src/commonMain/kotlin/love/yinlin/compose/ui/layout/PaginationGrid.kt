package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.platform.Platform

@Composable
fun <T> PaginationGrid(
    items: List<T>,
    key: ((T) -> Any)? = null,
    columns: GridCells,
    state: LazyGridState = rememberLazyGridState(),
    canRefresh: Boolean = true,
    canLoading: Boolean = false,
    onRefresh: (suspend () -> Unit)? = null,
    onLoading: (suspend () -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    header: (@Composable LazyGridItemScope.() -> Unit)? = null,
    itemContent: @Composable LazyGridItemScope.(T) -> Unit
) {
    if (Platform.contains(*Platform.Phone)) {
        SwipePaginationGrid(
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
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            header = header,
            itemContent = itemContent
        )
    }
    else {
        ClickPaginationGrid(
            items = items,
            key = key,
            columns = columns,
            state = state,
            canLoading = canLoading,
            onLoading = onLoading,
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            header = header,
            itemContent = itemContent
        )
    }
}