package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.extension.rememberState
import love.yinlin.extension.catching

@Composable
internal fun <T> ClickPaginationColumn(
    items: List<T>,
    key: ((T) -> Any)?,
    state: LazyListState,
    canLoading: Boolean,
    onLoading: (suspend () -> Unit)?,
    indicator: @Composable (PaginationStatus, () -> Unit) -> Unit = { status, onClick -> DefaultClickPaginationIndicator(status, onClick) },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    horizontalAlignment: Alignment.Horizontal,
    header: (@Composable LazyItemScope.() -> Unit)?,
    itemDivider: PaddingValues?,
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    val scope = rememberCoroutineScope()
    var status by rememberState { PaginationStatus.IDLE }

    LazyColumn(
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        modifier = modifier
    ) {
        if (header != null) {
            item(key = ItemKey("Header")) {
                header()
            }
        }

        itemsIndexed(items = items, key = key?.let { { _, item -> it(item) } }) {index, item->
            if (itemDivider != null && index != 0) HorizontalDivider(modifier = Modifier.padding(itemDivider))
            itemContent(item)
        }

        if (canLoading) {
            item(key = Unit) {
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