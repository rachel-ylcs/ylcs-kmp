package love.yinlin.compose.ui.layout

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun rememberReorderingState(
    state: LazyListState,
    onSettle: (from: Int, to: Int) -> Unit = { _, _ -> },
    onMove: (from: Int, to: Int) -> Unit,
): ReorderingState = rememberLazyReorderingState(state, onSettle, onMove)

@Composable
fun rememberReorderingState(
    state: LazyGridState,
    onSettle: (from: Int, to: Int) -> Unit = { _, _ -> },
    onMove: (from: Int, to: Int) -> Unit,
): ReorderingState = rememberLazyReorderingState(state, onSettle, onMove)

@Composable
fun rememberReorderingState(
    state: LazyStaggeredGridState,
    onSettle: (from: Int, to: Int) -> Unit = { _, _ -> },
    onMove: (from: Int, to: Int) -> Unit,
): ReorderingState = rememberLazyReorderingState(state, onSettle, onMove)

@Composable
private fun rememberLazyReorderingState(
    state: ScrollableState,
    onSettle: (Int, Int) -> Unit,
    onMove: (Int, Int) -> Unit,
): ReorderingState {
    val scope = rememberCoroutineScope()
    val direction = LocalLayoutDirection.current
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    val move = rememberUpdatedState(onMove)
    val settle = rememberUpdatedState(onSettle)
    val result = remember(state, scope, direction, threshold) {
        ReorderingState(ReorderingLazyLayout(state, direction), scope, threshold, move, settle)
    }

    DisposableEffect(result) {
        onDispose {
            result.cancelDrag()
        }
    }

    return result
}
