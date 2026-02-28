package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.extension.rememberState
import love.yinlin.extension.catching

@Composable
internal fun <T> ClickPaginationGrid(
    items: List<T>,
    key: ((T) -> Any)?,
    columns: GridCells,
    state: LazyGridState,
    canLoading: Boolean,
    onLoading: (suspend () -> Unit)?,
    indicator: @Composable (PaginationStatus, () -> Unit) -> Unit = { status, onClick -> DefaultClickPaginationIndicator(status, onClick) },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    header: (@Composable LazyGridItemScope.() -> Unit)?,
    itemContent: @Composable LazyGridItemScope.(T) -> Unit
) {
    val scope = rememberCoroutineScope()
    var status by rememberState { PaginationStatus.IDLE }

    LazyVerticalGrid(
        columns = columns,
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        horizontalArrangement = horizontalArrangement,
        modifier = modifier
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

        if (canLoading) {
            item(
                key = ItemKey("Loading"),
                span = { GridItemSpan(maxLineSpan) }
            ) {
                indicator(status) {
                    if (status != PaginationStatus.RUNNING) {
                        scope.launch {
                            status = PaginationStatus.RUNNING
                            catching { onLoading?.invoke() }
                            status = PaginationStatus.IDLE
                        }
                    }
                }
            }
        }
    }
}