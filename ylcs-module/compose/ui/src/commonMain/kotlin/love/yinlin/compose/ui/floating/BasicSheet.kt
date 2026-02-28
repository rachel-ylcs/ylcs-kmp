package love.yinlin.compose.ui.floating

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.layout.MeasureId
import love.yinlin.compose.ui.layout.MeasurePolicies
import love.yinlin.compose.ui.layout.find
import love.yinlin.compose.ui.layout.measureId
import love.yinlin.compose.ui.layout.require
import love.yinlin.compose.ui.node.condition
import kotlin.math.roundToInt

@Stable
abstract class BasicSheet<A : Any> internal constructor(): Floating<A>() {
    override fun alignment(device: Device): Alignment = when (device.type) {
        Device.Type.PORTRAIT -> Alignment.BottomCenter
        Device.Type.LANDSCAPE, Device.Type.SQUARE -> Alignment.CenterEnd
    }

    override fun enter(device: Device, animationSpeed: Int): EnterTransition = when (device.type) {
        Device.Type.PORTRAIT -> slideInVertically(
            animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing),
            initialOffsetY = { it }
        )
        Device.Type.LANDSCAPE, Device.Type.SQUARE -> slideInHorizontally(
            animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing),
            initialOffsetX = { it }
        )
    }

    override fun exit(device: Device, animationSpeed: Int): ExitTransition = when (device.type) {
        Device.Type.PORTRAIT -> slideOutVertically(
            targetOffsetY = { it }
        )
        Device.Type.LANDSCAPE, Device.Type.SQUARE -> slideOutHorizontally(
            animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing),
            targetOffsetX = { it }
        )
    }

    override val zIndex: Float = Z_INDEX_SHEET

    /**
     * 支持内容滚动
     */
    protected open val scrollable: Boolean = true

    // 竖屏尺寸比例
    protected open val minPortraitRatio: Float = 0.3f
    protected open val maxPortraitRatio: Float = 0.7f
    // 横屏固定宽度
    protected open val landscapeWidth: Dp? = null

    /**
     * 圆角形式(只有两个角)
     */
    protected open val usePortraitRoundedCorner: Boolean = true
    protected open val useLandscapeRoundedCorner: Boolean = false

    /**
     * 显示拖拽柄(仅在纵向模式)
     */
    protected open val showPortraitHandler: Boolean = true

    @Composable
    protected abstract fun Content(args: A)

    @Stable
    private class SheetController(
        private val portrait: Boolean,
        private val onClose: () -> Unit
    ) : NestedScrollConnection {
        var dimension by mutableIntStateOf(0)
        var offset by mutableIntStateOf(0)
            private set

        fun updateDelta(delta: Float) {
            offset = (offset + delta).roundToInt().coerceAtLeast(0)
        }

        fun stop() {
            if (offset > dimension / 2) onClose()
            else offset = 0
        }

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = if (portrait) available.y else available.x
            return if (source == NestedScrollSource.UserInput && offset > 0) {
                updateDelta(delta)
                available
            } else Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            val delta = if (portrait) available.y else available.x
            return if (source == NestedScrollSource.UserInput) {
                updateDelta(delta)
                available
            } else Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            stop()
            return available
        }
    }

    @Stable
    private enum class SheetMeasureId : MeasureId { HANDLER, CONTENT; }

    @Composable
    private fun PortraitSheet(args: A) {
        val controller = remember { SheetController(true, ::close) }
        val animatedOffset by animateIntAsState(targetValue = controller.offset)
        val shape = if (usePortraitRoundedCorner) Theme.shape.v1.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize) else Theme.shape.rectangle

        Surface(
            modifier = Modifier.fillMaxWidth()
                .offset { IntOffset(x = 0, y = animatedOffset) }
                .onSizeChanged { controller.dimension = it.height }
                .draggable(
                    state = rememberDraggableState { controller.updateDelta(it) },
                    orientation = Orientation.Vertical,
                    onDragStopped = { controller.stop() },
                ).nestedScroll(controller),
            shadowElevation = Theme.shadow.v1,
            tonalLevel = 5,
            contentPadding = LocalImmersivePadding.current,
            shape = shape
        ) {
            val handlerWidth = Theme.size.input8

            Layout(content = {
                if (showPortraitHandler) {
                    Layout(
                        modifier = Modifier.background(color = Theme.color.onSurfaceVariant, shape = Theme.shape.circle).measureId(SheetMeasureId.HANDLER),
                        measurePolicy = MeasurePolicies.Empty
                    )
                }
                Box(modifier = Modifier.measureId(SheetMeasureId.CONTENT).condition(scrollable) {
                    verticalScroll(state = rememberScrollState())
                }) {
                    Content(args)
                }
            }) { measurables, constraints ->
                val parentHeight = constraints.maxHeight
                val safeMaxRatio = maxPortraitRatio.coerceIn(0.1f, 1f)
                val safeMinRatio = minPortraitRatio.coerceIn(0.1f, maxPortraitRatio)
                val minHeight = (parentHeight * safeMinRatio).toInt()
                val maxHeight = (parentHeight * safeMaxRatio).toInt()
                val maxWidth = constraints.maxWidth

                val handlerMeasurable = measurables.find(SheetMeasureId.HANDLER)
                val contentMeasurable = measurables.require(SheetMeasureId.CONTENT)

                val handlerWidthPx = handlerWidth.roundToPx()
                val handlerHeightPx = handlerWidthPx / 8
                val handlerPlaceable = handlerMeasurable?.measure(Constraints.fixed(handlerWidthPx, handlerHeightPx))
                val contentStart = if (handlerPlaceable != null) handlerHeightPx * 5 else 0
                val contentPlaceable = contentMeasurable.measure(Constraints(
                    minWidth = 0,
                    maxWidth = maxWidth,
                    minHeight = minHeight - contentStart,
                    maxHeight = maxHeight - contentStart
                ))

                layout(maxWidth, contentStart + contentPlaceable.height) {
                    handlerPlaceable?.placeRelative(x = (maxWidth - handlerWidthPx) / 2, y = handlerHeightPx * 2)
                    contentPlaceable.placeRelative(x = 0, y = contentStart)
                }
            }
        }
    }

    @Composable
    private fun LandscapeSheet(args: A) {
        val controller = remember { SheetController(false, ::close) }
        val animatedOffset by animateIntAsState(targetValue = controller.offset)
        val shape = if (useLandscapeRoundedCorner) Theme.shape.v1.copy(topEnd = ZeroCornerSize, bottomEnd = ZeroCornerSize) else Theme.shape.rectangle
        val sheetWidth = landscapeWidth ?: Theme.size.sheet

        Surface(
            modifier = Modifier.width(sheetWidth).fillMaxHeight()
                .offset { IntOffset(x = animatedOffset, y = 0) }
                .onSizeChanged { controller.dimension = it.width }
                .draggable(
                    state = rememberDraggableState { controller.updateDelta(it) },
                    orientation = Orientation.Horizontal,
                    onDragStopped = { controller.stop() },
                ).nestedScroll(controller),
            shadowElevation = Theme.shadow.v1,
            tonalLevel = 5,
            contentAlignment = Alignment.TopStart,
            contentPadding = LocalImmersivePadding.current,
            shape = shape
        ) {
            Box(modifier = Modifier.condition(scrollable) {
                verticalScroll(state = rememberScrollState())
            }) {
                Content(args)
            }
        }
    }

    @Composable
    fun Land() {
        LandFloating { args ->
            when (LocalDevice.current.type) {
                Device.Type.PORTRAIT -> PortraitSheet(args)
                Device.Type.LANDSCAPE, Device.Type.SQUARE -> LandscapeSheet(args)
            }
        }
    }
}