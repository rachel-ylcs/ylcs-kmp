package love.yinlin.compose.ui.container

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberTrue

@Stable
@PublishedApi
internal data object DisableVerticalConnection : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
        if (source != NestedScrollSource.UserInput && available.y != 0f) Offset(x = 0f, y = available.y) else Offset.Zero
}

@Composable
@PublishedApi
internal inline fun HorizontalScrollContainer(
    state: ScrollableState,
    content: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: State<Boolean>,
    crossinline onConsume: suspend (Float) -> Unit,
) {
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier.pointerInput(state) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.first()
                    if (event.type == PointerEventType.Scroll && change.type == PointerType.Mouse) {
                        val delta = change.scrollDelta.y
                        if (delta != 0f) {
                            if (enabled.value) scope.launch { onConsume(delta) }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
        }.nestedScroll(DisableVerticalConnection),
        content = content
    )
}

@Composable
fun HorizontalScrollContainer(
    state: ScrollableState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    HorizontalScrollContainer(state, content, modifier, rememberTrue()) {
        state.scrollBy(it * 64f)
    }
}

@Composable
fun HorizontalScrollContainer(
    state: PagerState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    HorizontalScrollContainer(state, content, modifier, rememberDerivedState { !state.isScrollInProgress }) {
        val targetPage = if (it > 0) (state.currentPage + 1).coerceAtMost(state.pageCount - 1)
        else (state.currentPage - 1).coerceAtLeast(0)
        if (targetPage != state.currentPage) state.animateScrollToPage(targetPage)
    }
}