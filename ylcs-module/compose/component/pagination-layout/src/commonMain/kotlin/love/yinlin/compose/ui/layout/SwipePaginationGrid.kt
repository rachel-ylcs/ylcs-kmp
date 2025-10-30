package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.data.compose.ItemKey

@Composable
fun <T> SwipePaginationGrid(
    items: List<T>,
    key: ((T) -> Any)?,
    columns: GridCells,
    state: LazyGridState,
    canRefresh: Boolean,
    canLoading: Boolean,
    onRefresh: (suspend () -> Unit)?,
    onLoading: (suspend () -> Unit)?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    header: (@Composable LazyGridItemScope.() -> Unit)?,
    itemContent: @Composable LazyGridItemScope.(T) -> Unit
) {
    SwipePaginationLayout(
        canRefresh = canRefresh,
        canLoading = canLoading,
        onRefresh = onRefresh,
        onLoading = onLoading,
        modifier = modifier
    ) {
        LazyVerticalGrid(
            columns = columns,
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
        ) {
            if (header != null) {
                item(
                    key = ItemKey("Header"),
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    header()
                }
            }
            items(items = items, key = key, itemContent = itemContent)
        }
    }
}