package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.ui.container.itemKey

@Composable
fun <T> PaginationStaggeredGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    columns: StaggeredGridCells,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    canRefresh: Boolean = true,
    canLoading: Boolean = false,
    onRefresh: (suspend () -> Unit)? = null,
    onLoading: (suspend () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues.Zero,
    verticalItemSpacing: Dp = Dp.Hairline,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    header: (@Composable LazyStaggeredGridItemScope.() -> Unit)? = null,
    itemContent: @Composable LazyStaggeredGridItemScope.(T) -> Unit
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
        LazyVerticalStaggeredGrid(
            modifier = Modifier.fillMaxSize(),
            columns = columns,
            state = state,
            contentPadding = contentPadding,
            verticalItemSpacing = verticalItemSpacing,
            horizontalArrangement = horizontalArrangement
        ) {
            if (header != null) {
                itemKey("Header", span = StaggeredGridItemSpan.FullLine) {
                    header()
                }
            }

            items(items = items, key = key, itemContent = itemContent)
        }
    }
}