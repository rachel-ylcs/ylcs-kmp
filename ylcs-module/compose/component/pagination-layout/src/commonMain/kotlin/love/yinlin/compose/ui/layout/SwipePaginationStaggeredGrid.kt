package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import love.yinlin.data.compose.ItemKey

@Composable
internal fun <T> SwipePaginationStaggeredGrid(
    items: List<T>,
    key: ((T) -> Any)?,
    columns: StaggeredGridCells,
    state: LazyStaggeredGridState,
    canRefresh: Boolean,
    canLoading: Boolean,
    onRefresh: (suspend () -> Unit)?,
    onLoading: (suspend () -> Unit)?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    verticalItemSpacing: Dp,
    horizontalArrangement: Arrangement.Horizontal,
    header: (@Composable LazyStaggeredGridItemScope.() -> Unit)?,
    itemContent: @Composable LazyStaggeredGridItemScope.(T) -> Unit
) {
    SwipePaginationLayout(
        canRefresh = canRefresh,
        canLoading = canLoading,
        onRefresh = onRefresh,
        onLoading = onLoading,
        modifier = modifier
    ) {
        LazyVerticalStaggeredGrid(
            columns = columns,
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = contentPadding,
            verticalItemSpacing = verticalItemSpacing,
            horizontalArrangement = horizontalArrangement
        ) {
            if (header != null) {
                item(
                    key = ItemKey("Header"),
                    span = StaggeredGridItemSpan.FullLine
                ) {
                    header()
                }
            }
            items(items = items, key = key, itemContent = itemContent)
        }
    }
}