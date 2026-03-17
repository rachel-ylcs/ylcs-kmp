package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.extension.rememberDerivedState

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
    val internalCanRefresh = rememberDerivedState(canRefresh) {
        canRefresh && state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0
    }
    val internalCanLoading = rememberDerivedState(canLoading) {
        canLoading && !state.canScrollForward
    }

    PullLayout(
        canRefresh = internalCanRefresh,
        canLoading = internalCanLoading,
        onRefresh = onRefresh,
        onLoading = onLoading,
        modifier = modifier,
    ) {
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = columns,
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