package love.yinlin.ui.component.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import love.yinlin.extension.rememberValueState
import love.yinlin.platform.app
import kotlin.math.roundToInt

// SheetConfig仅在竖屏下生效
@Stable
data class SheetConfig(
    // 竖屏配置
    val min: Float = 0.3f,
    val max: Float = 0.7f,
    val full: Boolean = false,
    // 横屏配置
    val maxWidth: Dp = 400.dp
)

@Suppress("DuplicatedCode")
@Stable
open class FloatingArgsSheet<A : Any>(private val config: SheetConfig = SheetConfig()) : Floating<A>() {
    override val alignment: Alignment = if (app.isPortrait) Alignment.BottomCenter else Alignment.CenterEnd
    override val enter: EnterTransition = if (app.isPortrait) slideInVertically(
        animationSpec = tween(durationMillis = duration, easing = LinearOutSlowInEasing),
        initialOffsetY = { it }
    ) else slideInHorizontally(
        animationSpec = tween(durationMillis = duration, easing = LinearOutSlowInEasing),
        initialOffsetX = { it }
    )
    override val exit: ExitTransition = if (app.isPortrait) slideOutVertically(
        animationSpec = tween(durationMillis = duration, easing = LinearOutSlowInEasing),
        targetOffsetY = { it }
    ) else slideOutHorizontally(
        animationSpec = tween(durationMillis = duration, easing = LinearOutSlowInEasing),
        targetOffsetX = { it }
    )
    override val zIndex: Float get() = Z_INDEX_SHEET

    @Composable
    private fun PortraitWrapperContent(maxHeight: Dp, block: @Composable () -> Unit) {
        Column(modifier = Modifier.fillMaxWidth()
            .then(Modifier.heightIn(maxHeight * config.min, maxHeight * config.max))
            .then(if (config.full) Modifier.fillMaxHeight() else Modifier)
        ) {
            // DragHandler
            Surface(
                modifier = Modifier.padding(vertical = 10.dp).align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(Modifier.size(width = 32.dp, height = 4.dp))
            }
            // Content
            block()
        }
    }

    @Composable
    private fun PortraitWrapper(block: @Composable () -> Unit) {
        var height by rememberValueState(0)
        var offset by rememberValueState(0)
        val animatedOffset by animateIntAsState(targetValue = offset)

        val onDelta = { delta: Float -> offset = ((offset + delta).roundToInt()).coerceAtLeast(0) }
        val onStop = { if (offset > height / 2) close() else offset = 0 }

        Surface(
            shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0), bottomEnd = CornerSize(0)),
            modifier = Modifier.fillMaxWidth()
                .offset { IntOffset(x = 0, y = animatedOffset) }
                .onSizeChanged { height = it.height }
                .draggable(
                    state = rememberDraggableState(onDelta),
                    orientation = Orientation.Vertical,
                    onDragStopped = { onStop() }
                )
                .nestedScroll(connection = remember { object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                        if (source == NestedScrollSource.UserInput && offset > 0) {
                            onDelta(available.y)
                            available
                        } else Offset.Zero

                    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                        if (source == NestedScrollSource.UserInput) {
                            onDelta(available.y)
                            available
                        } else Offset.Zero

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        onStop()
                        return available
                    }
                } })
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                PortraitWrapperContent(maxHeight, block)
            }
        }
    }

    @Composable
    private fun LandscapeWrapperContent(block: @Composable () -> Unit) {
        Row(modifier = Modifier.fillMaxSize()) {
            // DragHandler
            Surface(
                modifier = Modifier.padding(horizontal = 10.dp).align(Alignment.CenterVertically),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(Modifier.size(width = 4.dp, height = 32.dp))
            }
            // Content
            block()
        }
    }

    @Composable
    private fun LandscapeWrapper(block: @Composable () -> Unit) {
        val widthPx = with(LocalDensity.current) { config.maxWidth.toPx() }
        var offset by rememberValueState(0)
        val animatedOffset by animateIntAsState(targetValue = offset)

        val onDelta = { delta: Float -> offset = ((offset + delta).roundToInt()).coerceAtLeast(0) }
        val onStop = { if (offset > widthPx / 2) close() else offset = 0 }

        Surface(
            shadowElevation = 5.dp,
            shape = MaterialTheme.shapes.extraLarge.copy(topEnd = CornerSize(0), bottomEnd = CornerSize(0)),
            modifier = Modifier.width(config.maxWidth).fillMaxHeight()
                .offset { IntOffset(x = animatedOffset, y = 0) }
                .draggable(
                    state = rememberDraggableState(onDelta),
                    orientation = Orientation.Horizontal,
                    onDragStopped = { onStop() }
                )
                .nestedScroll(connection = remember { object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                        if (source == NestedScrollSource.UserInput && offset > 0) {
                            onDelta(available.x)
                            available
                        } else Offset.Zero

                    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                        if (source == NestedScrollSource.UserInput) {
                            onDelta(available.x)
                            available
                        } else Offset.Zero

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        onStop()
                        return available
                    }
                } })
        ) {
            LandscapeWrapperContent(block)
        }
    }

    @Composable
    override fun Wrapper(block: @Composable () -> Unit) {
        if (app.isPortrait) PortraitWrapper(block)
        else LandscapeWrapper(block)
    }
}

@Stable
class FloatingSheet(config: SheetConfig = SheetConfig()) : FloatingArgsSheet<Unit>(config) {
    fun open() { open(Unit) }
}