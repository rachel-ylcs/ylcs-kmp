package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.data.ItemKey

@Composable
fun <T> PaginationColumn(
    items: List<T>,
    key: ((T) -> Any)? = null,
    state: LazyListState = rememberLazyListState(),
    canRefresh: Boolean = true,
    canLoading: Boolean = false,
    onRefresh: (suspend () -> Unit)? = null,
    onLoading: (suspend () -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    header: (@Composable LazyItemScope.() -> Unit)? = null,
    itemDivider: (@Composable () -> Unit)? = null,
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    PullLayout(
        canContainerRefresh = { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 },
        canContainerLoading = { !state.canScrollForward },
        canRefresh = canRefresh,
        canLoading = canLoading,
        onRefresh = onRefresh,
        onLoading = onLoading,
        modifier = modifier,
    ) {
        LazyColumn(
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
        ) {
            if (header != null) {
                item(key = ItemKey("Header")) {
                    header()
                }
            }

            itemsIndexed(items = items, key = key?.let { { _, item -> it(item) } }) {index, item->
                itemContent(item)
                if (itemDivider != null && index != items.lastIndex) itemDivider()
            }
        }
    }
}