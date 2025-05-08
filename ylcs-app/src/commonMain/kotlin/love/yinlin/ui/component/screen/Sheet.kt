package love.yinlin.ui.component.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
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
import love.yinlin.common.Device
import love.yinlin.common.LocalDevice
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.extension.rememberValueState
import love.yinlin.platform.app
import kotlin.math.roundToInt

// SheetConfig仅在竖屏下生效
@Stable
data class SheetConfig(
    // 竖屏配置
    val min: Float = 0.3f,
    val max: Float = 0.7f,
    val full: Boolean = false
)

@Suppress("DuplicatedCode")
@Stable
open class FloatingArgsSheet<A : Any>(private val config: SheetConfig = SheetConfig()) : Floating<A>() {
    override fun alignment(device: Device): Alignment = when (device.type) {
        Device.Type.PORTRAIT -> Alignment.BottomCenter
        Device.Type.LANDSCAPE, Device.Type.SQUARE -> Alignment.CenterEnd
    }
    override fun enter(device: Device): EnterTransition = when (device.type) {
        Device.Type.PORTRAIT -> slideInVertically(
            animationSpec = tween(durationMillis = app.config.animationSpeed, easing = LinearOutSlowInEasing),
            initialOffsetY = { it }
        )
        Device.Type.LANDSCAPE, Device.Type.SQUARE -> slideInHorizontally(
            animationSpec = tween(durationMillis = app.config.animationSpeed, easing = LinearOutSlowInEasing),
            initialOffsetX = { it }
        )
    }
    override fun exit(device: Device): ExitTransition = when (device.type) {
        Device.Type.PORTRAIT -> slideOutVertically(
            animationSpec = tween(durationMillis = app.config.animationSpeed, easing = LinearOutSlowInEasing),
            targetOffsetY = { it }
        )
        Device.Type.LANDSCAPE, Device.Type.SQUARE -> slideOutHorizontally(
            animationSpec = tween(durationMillis = app.config.animationSpeed, easing = LinearOutSlowInEasing),
            targetOffsetX = { it }
        )
    }
    override val zIndex: Float get() = Z_INDEX_SHEET

    @Composable
    private fun PortraitWrapperContent(block: @Composable () -> Unit) {
        BoxWithConstraints(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()
                .then(Modifier.heightIn(maxHeight * config.min, maxHeight * config.max))
                .then(if (config.full) Modifier.fillMaxHeight() else Modifier)
            ) {
                // DragHandler
                Surface(
                    modifier = Modifier.padding(vertical = ThemeValue.Padding.EqualSpace).align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Box(Modifier.size(width = ThemeValue.Size.ExtraIcon, height = ThemeValue.Size.ExtraIcon / 8))
                }
                // Content
                block()
            }
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
                    orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
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
            PortraitWrapperContent(block)
        }
    }

    @Composable
    private fun LandscapeWrapperContent(block: @Composable () -> Unit) {
        Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            // DragHandler
            Surface(
                modifier = Modifier.padding(horizontal = ThemeValue.Padding.EqualSpace).align(Alignment.CenterVertically),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(Modifier.size(width = ThemeValue.Size.ExtraIcon / 8, height = ThemeValue.Size.ExtraIcon))
            }
            // Content
            block()
        }
    }

    @Composable
    private fun LandscapeWrapper(block: @Composable () -> Unit) {
        val maxWidth = ThemeValue.Size.SheetWidth
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        var offset by rememberValueState(0)
        val animatedOffset by animateIntAsState(targetValue = offset)

        val onDelta = { delta: Float -> offset = ((offset + delta).roundToInt()).coerceAtLeast(0) }
        val onStop = { if (offset > widthPx / 2) close() else offset = 0 }

        Surface(
            shadowElevation = ThemeValue.Shadow.Surface,
            shape = MaterialTheme.shapes.extraLarge.copy(topEnd = CornerSize(0), bottomEnd = CornerSize(0)),
            modifier = Modifier.width(maxWidth).fillMaxHeight()
                .offset { IntOffset(x = animatedOffset, y = 0) }
                .draggable(
                    state = rememberDraggableState(onDelta),
                    orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
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
        when (LocalDevice.current.type) {
            Device.Type.PORTRAIT -> PortraitWrapper(block)
            Device.Type.LANDSCAPE, Device.Type.SQUARE -> LandscapeWrapper(block)
        }
    }
}

@Stable
class FloatingSheet(config: SheetConfig = SheetConfig()) : FloatingArgsSheet<Unit>(config) {
    fun open() { open(Unit) }
}