package love.yinlin.compose.ui.container

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.Composable
import love.yinlin.compose.data.ItemKey

fun LazyListScope.itemKey(value: String, contentType: Any? = null, content: @Composable LazyItemScope.() -> Unit) =
    item(key = ItemKey(value), contentType = contentType, content = content)

fun LazyGridScope.itemKey(value: String, contentType: Any? = null, span: (LazyGridItemSpanScope.() -> GridItemSpan)? = null, content: @Composable LazyGridItemScope.() -> Unit) =
    item(key = ItemKey(value), contentType = contentType, span = span, content = content)

fun LazyStaggeredGridScope.itemKey(value: String, contentType: Any? = null, span: StaggeredGridItemSpan? = null, content: @Composable LazyStaggeredGridItemScope.() -> Unit) =
    item(key = ItemKey(value), contentType = contentType, span = span, content = content)
