package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.data.compose.ItemKey

@Composable
internal fun <T> SwipePaginationColumn(
    items: List<T>,
    key: ((T) -> Any)?,
    state: LazyListState,
    canRefresh: Boolean,
    canLoading: Boolean,
    onRefresh: (suspend () -> Unit)?,
    onLoading: (suspend () -> Unit)?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    horizontalAlignment: Alignment.Horizontal,
    header: (@Composable LazyItemScope.() -> Unit)?,
    itemDivider: PaddingValues?,
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    SwipePaginationLayout(
        canRefresh = canRefresh,
        canLoading = canLoading,
        onRefresh = onRefresh,
        onLoading = onLoading,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                if (itemDivider != null && index != 0) HorizontalDivider(modifier = Modifier.padding(itemDivider))
                itemContent(item)
            }
        }
    }
}