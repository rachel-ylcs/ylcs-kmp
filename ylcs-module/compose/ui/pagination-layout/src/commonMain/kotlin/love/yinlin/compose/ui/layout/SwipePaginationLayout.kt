package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import love.yinlin.compose.ui.CustomTheme
import kotlin.math.abs
import kotlin.math.absoluteValue

@Composable
internal fun SwipePaginationLayout(
    canRefresh: Boolean = true,
    canLoading: Boolean = false,
    onRefresh: (suspend () -> Unit)? = null,
    onLoading: (suspend () -> Unit)? = null,
    headerHeight: Dp = CustomTheme.size.refreshHeaderHeight,
    header: @Composable (PaginationStatus, Float) -> Unit = { status, progress -> DefaultSwipePaginationHeader(status, progress) },
    footerHeight: Dp = CustomTheme.size.refreshFooterHeight,
    footer: @Composable (PaginationStatus, Float) -> Unit = { status, progress -> DefaultSwipePaginationFooter(status, progress) },
    stickinessLevel: Float = 0.5f,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val state = remember { SwipeState() }
    val (headerHeightPx, footerHeightPx) = with(LocalDensity.current) {
        headerHeight.toPx() to footerHeight.toPx()
    }
    val connection = remember(
        canRefresh, canLoading, onRefresh, onLoading, stickinessLevel, state, headerHeightPx, footerHeightPx
    ) { object : NestedScrollConnection {
        private fun scroll(canConsumed: Float): Offset = if (canConsumed.absoluteValue > 0.5f) {
            scope.launch {
                state.snapOffsetTo(headerHeightPx, footerHeightPx, state.indicatorOffset + canConsumed)
            }
            Offset(0f, canConsumed / stickinessLevel)
        } else Offset.Zero

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = when {
            state.isRunning -> Offset.Zero
            available.y < 0 && state.indicatorOffset > 0 -> scroll((available.y * stickinessLevel).coerceAtLeast(-state.indicatorOffset))
            available.y > 0 && state.indicatorOffset < 0 -> scroll((available.y * stickinessLevel).coerceAtMost(-state.indicatorOffset))
            else -> Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = when {
            state.isRunning -> Offset.Zero
            available.y > 0 && canRefresh -> scroll(canConsumed =
                if (source == NestedScrollSource.SideEffect) (available.y * stickinessLevel).coerceAtMost(-state.indicatorOffset)
                else (available.y * stickinessLevel).coerceAtMost(headerHeightPx - state.indicatorOffset)
            )
            available.y < 0 && canLoading -> scroll(canConsumed =
                if (source == NestedScrollSource.SideEffect) (available.y * stickinessLevel).coerceAtLeast(-state.indicatorOffset)
                else (available.y * stickinessLevel).coerceAtLeast(-footerHeightPx - state.indicatorOffset)
            )
            else -> Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (state.isRunning) return Velocity.Zero
            state.isReleaseEdge = state.indicatorOffset != 0f
            return if (state.indicatorOffset >= headerHeightPx && state.isReleaseEdge && state.refreshStatus != PaginationStatus.RUNNING) {
                state.isAnimateOver = false
                state.refreshStatus = PaginationStatus.RUNNING
                state.animateOffsetTo(headerHeightPx)
                onRefresh?.invoke()
                state.refreshStatus = PaginationStatus.IDLE
                state.animateOffsetTo(0f)
                available
            }
            else if (state.indicatorOffset <= -footerHeightPx && state.isReleaseEdge && state.loadingStatus != PaginationStatus.RUNNING) {
                state.isAnimateOver = false
                state.loadingStatus = PaginationStatus.RUNNING
                state.animateOffsetTo(-footerHeightPx)
                onLoading?.invoke()
                state.loadingStatus = PaginationStatus.IDLE
                state.animateOffsetTo(0f)
                available
            }
            else super.onPreFling(available)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (state.isRunning) return Velocity.Zero
            if (state.indicatorOffset > 0 && state.refreshStatus != PaginationStatus.RUNNING) {
                state.refreshStatus = PaginationStatus.IDLE
                state.animateOffsetTo(0f)
            }
            else if (state.indicatorOffset < 0 && state.loadingStatus != PaginationStatus.RUNNING) {
                state.loadingStatus = PaginationStatus.IDLE
                state.animateOffsetTo(0f)
            }
            return super.onPostFling(consumed, available)
        }
    } }

    Box(modifier = modifier.clipToBounds().nestedScroll(connection)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(headerHeight).align(Alignment.TopCenter)
                .graphicsLayer { translationY = -headerHeightPx + state.indicatorOffset }
        ) {
            header(state.refreshStatus, abs(state.indicatorOffset) / headerHeightPx)
        }
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer { translationY = state.indicatorOffset },
            content = content
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(footerHeight).align(Alignment.BottomCenter)
                .graphicsLayer { translationY = footerHeightPx + state.indicatorOffset }
        ) {
            footer(state.loadingStatus, -state.indicatorOffset / footerHeightPx)
        }
    }
}