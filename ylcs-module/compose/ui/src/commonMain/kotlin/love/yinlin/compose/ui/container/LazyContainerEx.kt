package love.yinlin.compose.ui.container

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.runtime.Composable
import love.yinlin.compose.data.ItemKey

fun LazyListScope.itemKey(value: String, content: @Composable LazyItemScope.() -> Unit) =
    item(key = ItemKey(value), contentType = null, content = content)

fun LazyGridScope.itemKey(value: String, content: @Composable LazyGridItemScope.() -> Unit) =
    item(key = ItemKey(value), contentType = null, content = content)

fun LazyStaggeredGridScope.itemKey(value: String, content: @Composable LazyStaggeredGridItemScope.() -> Unit) =
    item(key = ItemKey(value), contentType = null, content = content)