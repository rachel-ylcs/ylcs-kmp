package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import love.yinlin.compose.data.ItemKey

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
    PullLayout(
        canRefresh = canRefresh,
        canLoading = canLoading,
        onRefresh = onRefresh,
        onLoading = onLoading
    ) {
        LazyVerticalStaggeredGrid(
            columns = columns,
            modifier = modifier,
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