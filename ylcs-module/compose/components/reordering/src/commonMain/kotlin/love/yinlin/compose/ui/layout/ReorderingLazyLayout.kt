package love.yinlin.compose.ui.layout

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import love.yinlin.compose.ui.layout.ReorderingLayout.Info
import love.yinlin.compose.ui.layout.ReorderingLayout.Item

internal class ReorderingLazyLayout(
    private val state: ScrollableState,
    private val layoutDirection: LayoutDirection,
) : ReorderingLayout {
    private var lastLayout: Any? = null
    private var lastInfo: Info? = null

    override val info: Info
        get() = when (state) {
            is LazyListState -> {
                val layout = state.layoutInfo
                cached(layout) {
                    val vertical = layout.orientation == Orientation.Vertical
                    Info(
                        items = layout.visibleItemsInfo.map {
                            Item(
                                index = it.index,
                                key = it.key,
                                offset = if (vertical) Offset(0f, it.offset.toFloat()) else Offset(it.offset.toFloat(), 0f),
                                size = if (vertical) Size(0f, it.size.toFloat()) else Size(it.size.toFloat(), 0f)
                            )
                        },
                        orientation = layout.orientation,
                        viewportSize = (if (vertical) layout.viewportSize.height else layout.viewportSize.width).toFloat(),
                        beforeContentPadding = layout.beforeContentPadding,
                        totalItemsCount = layout.totalItemsCount,
                        firstVisibleIndex = state.firstVisibleItemIndex,
                        mainAxisReversed = layout.reverseLayout xor (!vertical && layoutDirection == LayoutDirection.Rtl),
                        canScrollBackward = state.canScrollBackward,
                        canScrollForward = state.canScrollForward,
                        isList = true,
                        crossAxisSize = (if (vertical) layout.viewportSize.width else layout.viewportSize.height).toFloat(),
                    )
                }
            }
            is LazyGridState -> {
                val layout = state.layoutInfo
                cached(layout) {
                    val vertical = layout.orientation == Orientation.Vertical
                    Info(
                        items = layout.visibleItemsInfo.map {
                            Item(
                                index = it.index,
                                key = it.key,
                                offset = it.offset.toOffset(),
                                size = it.size.toSize()
                            )
                        },
                        orientation = layout.orientation,
                        viewportSize = (if (vertical) layout.viewportSize.height else layout.viewportSize.width).toFloat(),
                        beforeContentPadding = layout.beforeContentPadding,
                        totalItemsCount = layout.totalItemsCount,
                        firstVisibleIndex = state.firstVisibleItemIndex,
                        mainAxisReversed = layout.reverseLayout xor (!vertical && layoutDirection == LayoutDirection.Rtl),
                        canScrollBackward = state.canScrollBackward,
                        canScrollForward = state.canScrollForward,
                        crossAxisSize = (if (vertical) layout.viewportSize.width else layout.viewportSize.height).toFloat(),
                    )
                }
            }
            is LazyStaggeredGridState -> {
                val layout = state.layoutInfo
                cached(layout) {
                    val vertical = layout.orientation == Orientation.Vertical
                    Info(
                        items = layout.visibleItemsInfo.map {
                            Item(
                                index = it.index,
                                key = it.key,
                                offset = it.offset.toOffset(),
                                size = it.size.toSize()
                            )
                        },
                        orientation = layout.orientation,
                        viewportSize = (if (vertical) layout.viewportSize.height else layout.viewportSize.width).toFloat(),
                        beforeContentPadding = layout.beforeContentPadding,
                        totalItemsCount = layout.totalItemsCount,
                        firstVisibleIndex = state.firstVisibleItemIndex,
                        mainAxisReversed = layout.reverseLayout xor (!vertical && layoutDirection == LayoutDirection.Rtl),
                        crossAxisReversed = vertical && layoutDirection == LayoutDirection.Rtl,
                        canScrollBackward = state.canScrollBackward,
                        canScrollForward = state.canScrollForward,
                        crossAxisSize = (if (vertical) layout.viewportSize.width else layout.viewportSize.height).toFloat(),
                    )
                }
            }
            else -> error("Unsupported reordering layout")
        }

    private inline fun cached(layout: Any, block: () -> Info): Info {
        if (lastLayout !== layout) {
            lastInfo = block()
            lastLayout = layout
        }
        return lastInfo!!
    }

    override fun prepareMove(from: Int, to: Int) {
        when (state) {
            is LazyListState -> if (from == state.firstVisibleItemIndex || to == state.firstVisibleItemIndex) {
                state.requestScrollToItem(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
            }
            is LazyGridState -> if (from == state.firstVisibleItemIndex || to == state.firstVisibleItemIndex) {
                state.requestScrollToItem(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
            }
            is LazyStaggeredGridState -> if (from == state.firstVisibleItemIndex || to == state.firstVisibleItemIndex) {
                state.requestScrollToItem(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
            }
        }
    }

    override suspend fun stopScroll() = state.stopScroll(MutatePriority.UserInput)

    override suspend fun scrollBy(value: Float): Float {
        var consumed = 0f
        state.scroll(MutatePriority.UserInput) { consumed = scrollBy(value) }
        return consumed
    }
}
