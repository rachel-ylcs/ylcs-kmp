package love.yinlin.compose.ui.floating

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import love.yinlin.compose.ui.icon.Icons

@Stable
class FABScrollTop private constructor(statusProvider: () -> Boolean, scrollProvider: suspend (Int, Int) -> Unit) : FAB() {
    constructor(state: LazyListState) : this(
        { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 },
        state::animateScrollToItem
    )
    constructor(state: LazyGridState) : this(
        { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 },
        state::animateScrollToItem
    )
    constructor(state: LazyStaggeredGridState) : this(
        { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 },
        state::animateScrollToItem
    )

    private val isScrollTop: Boolean by derivedStateOf(statusProvider)

    override val action: FABAction? by derivedStateOf {
        if (isScrollTop) null else FABAction(
            iconProvider = { Icons.ArrowUpward },
            onClick = { scrollProvider(0, 0) }
        )
    }
}