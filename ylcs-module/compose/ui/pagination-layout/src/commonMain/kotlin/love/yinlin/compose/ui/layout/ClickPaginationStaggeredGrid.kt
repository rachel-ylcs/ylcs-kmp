package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.extension.rememberState

@Composable
internal fun <T> ClickPaginationStaggeredGrid(
    items: List<T>,
    key: ((T) -> Any)?,
    columns: StaggeredGridCells,
    state: LazyStaggeredGridState,
    canLoading: Boolean = false,
    onLoading: (suspend () -> Unit)?,
    indicator: @Composable (PaginationStatus, () -> Unit) -> Unit = { status, onClick -> DefaultClickPaginationIndicator(status, onClick) },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    verticalItemSpacing: Dp,
    horizontalArrangement: Arrangement.Horizontal,
    header: (@Composable LazyStaggeredGridItemScope.() -> Unit)?,
    itemContent: @Composable LazyStaggeredGridItemScope.(T) -> Unit
) {
    val scope = rememberCoroutineScope()
    var status by rememberState { PaginationStatus.IDLE }
    LazyVerticalStaggeredGrid(
        columns = columns,
        state = state,
        contentPadding = contentPadding,
        verticalItemSpacing = verticalItemSpacing,
        horizontalArrangement = horizontalArrangement,
        modifier = modifier
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
        if (canLoading) {
            item(
                key = Unit,
                span = StaggeredGridItemSpan.FullLine
            ) {
                indicator(status) {
                    if (status != PaginationStatus.RUNNING) {
                        scope.launch {
                            status = PaginationStatus.RUNNING
                            onLoading?.invoke()
                            status = PaginationStatus.IDLE
                        }
                    }
                }
            }
        }
    }
}