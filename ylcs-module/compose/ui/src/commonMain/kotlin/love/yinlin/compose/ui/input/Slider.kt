package love.yinlin.compose.ui.input

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.layout.MeasureId
import love.yinlin.compose.ui.layout.MeasurePolicies
import love.yinlin.compose.ui.layout.find
import love.yinlin.compose.ui.layout.measureId
import love.yinlin.compose.ui.layout.require
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.shadow
import kotlin.math.roundToInt

@Stable
private enum class SliderMeasureId : MeasureId {
    Track, ActiveTrack, Thumb, Content;
}

@Stable
private class SliderMeasurePolicy(val percent: Float, val trackHeight: Dp, val minWidth: Dp) : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val trackHeightPx = trackHeight.roundToPx()
        val desiredMinWidth = minWidth.roundToPx()
        val layoutWidth = constraints.constrainWidth(desiredMinWidth)
        val desiredHeight = trackHeightPx * 2
        val layoutHeight = constraints.constrainHeight(desiredHeight)

        val trackPlaceable = measurables.require(SliderMeasureId.Track).measure(
            Constraints.fixed(layoutWidth, trackHeightPx)
        )
        val activeTrackWidth = (layoutWidth * percent).roundToInt()
        val activeTrackPlaceable = measurables.require(SliderMeasureId.ActiveTrack).measure(
            Constraints.fixed(activeTrackWidth, trackHeightPx)
        )
        val thumbPlaceable = measurables.find(SliderMeasureId.Thumb)?.measure(
            Constraints.fixed(desiredHeight, desiredHeight)
        )
        val contentPlaceable = measurables.find(SliderMeasureId.Content)?.measure(
            Constraints.fixed(layoutWidth, layoutHeight)
        )

        return layout(layoutWidth, layoutHeight) {
            val centerY = layoutHeight / 2

            contentPlaceable?.placeRelative(0, 0)

            val trackY = centerY - (trackHeightPx / 2)
            trackPlaceable.placeRelative(0, trackY)
            activeTrackPlaceable.placeRelative(0, trackY)

            if (thumbPlaceable != null) {
                val thumbY = centerY - (desiredHeight / 2)
                val thumbCenterX = layoutWidth * percent
                val thumbX = (thumbCenterX - desiredHeight / 2).roundToInt()
                thumbPlaceable.placeRelative(thumbX, thumbY)
            }
        }
    }
}

/**
 * @param value 当前进度 (0 ~ 1)
 * @param onValueChangeFinished 拖拽结束事件
 * @param onValueChange 拖拽事件 (进度变化频繁)
 * @param enabled 启用
 * @param trackHeight 轨高
 * @param trackColor 背景色
 * @param activeColor 激活色
 * @param trackShape 轨形状
 * @param showThumb 是否显示拖纽
 * @param thumbColor 拖钮颜色
 * @param content 其他叠加内容
 */
@Composable
fun Slider(
    value: Float,
    onValueChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChange: ((Float) -> Unit)? = null,
    enabled: Boolean = true,
    trackHeight: Dp = Theme.size.box3,
    trackColor: Color = Theme.color.backgroundVariant,
    activeColor: Color = Theme.color.primaryContainer,
    trackShape: Shape = Theme.shape.v7,
    showThumb: Boolean = true,
    thumbColor: Color = Theme.color.onContainer,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    val minWidth = Theme.size.input5

    val actualTrackColor = if (enabled) trackColor else Theme.color.disabledContainer
    val actualActiveColor = if (enabled) activeColor else Theme.color.disabledContent
    val actualThumbColor = if (enabled) thumbColor else Theme.color.onContainerVariant

    val updatedOnValueChange by rememberUpdatedState(onValueChange)
    val updatedOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    var isDragging by rememberFalse()
    var currentPercent by rememberValueState(value)

    LaunchedEffect(value) {
        val newValue = value.coerceIn(0f, 1f)
        if (!isDragging && currentPercent != newValue) currentPercent = newValue
    }

    Layout(
        content = {
            // 1. Track
            Box(modifier = Modifier.measureId(SliderMeasureId.Track).shadow(trackShape, Theme.shadow.v9).background(actualTrackColor, trackShape))
            // 2. ActiveTrack
            Box(modifier = Modifier.measureId(SliderMeasureId.ActiveTrack).background(actualActiveColor, trackShape))
            // 3. Thumb
            if (showThumb) {
                val dotRatio by animateFloatAsState(if (isDragging) 0.6f else 0.33333f)

                Layout(modifier = Modifier.measureId(SliderMeasureId.Thumb).shadow(Theme.shape.circle, Theme.shadow.v9).drawBehind {
                    val radius = this.size.width / 2
                    drawCircle(actualThumbColor, radius)
                    drawCircle(actualActiveColor, radius * dotRatio)
                }, measurePolicy = MeasurePolicies.Empty)
            }
            // 4. Content
            if (content != null) {
                Box(modifier = Modifier.measureId(SliderMeasureId.Content)) {
                    content()
                }
            }
        },
        modifier = modifier.pointerIcon(PointerIcon.Hand, enabled).pointerInput(enabled) {
            if (!enabled) return@pointerInput

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)

                isDragging = true
                val width = size.width.toFloat()
                if (width > 0) {
                    val newPercent = (down.position.x / width).coerceIn(0f, 1f)
                    currentPercent = newPercent
                    updatedOnValueChange?.invoke(newPercent)
                }

                var dragChange = down
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (change.pressed != dragChange.pressed) break

                    if (change.positionChange() != Offset.Zero) {
                        val newPercent = (change.position.x / width).coerceIn(0f, 1f)
                        currentPercent = newPercent
                        updatedOnValueChange?.invoke(newPercent)
                        change.consume()
                    }
                    dragChange = change
                } while (dragChange.pressed)

                isDragging = false
                updatedOnValueChangeFinished(currentPercent)
            }
        },
        measurePolicy = remember(currentPercent, trackHeight, minWidth) {
            SliderMeasurePolicy(
                percent = currentPercent,
                trackHeight = trackHeight,
                minWidth = minWidth
            )
        }
    )
}

@Stable
interface SliderConverter<T> {
    fun from(value: T): Float
    fun to(value: Float): T
}

@Stable
data class SliderIntConverter(val min: Int, val max: Int) : SliderConverter<Int> {
    override fun from(value: Int): Float = if (min == max) 0f else (value - min).toFloat() / (max - min)
    override fun to(value: Float): Int = (min + (max - min) * value).roundToInt()
}

@Stable
data class SliderFloatConverter(val min: Float, val max: Float) : SliderConverter<Float> {
    override fun from(value: Float): Float = if (min == max) 0f else (value - min) / (max - min)
    override fun to(value: Float): Float = min + (max - min) * value
}

@Stable
data class SliderDpConverter(val min: Dp, val max: Dp) : SliderConverter<Dp> {
    override fun from(value: Dp): Float = if (min == max) 0f else (value - min) / (max - min)
    override fun to(value: Float): Dp = min + (max - min) * value
}

@Composable
fun <T> Slider(
    value: T,
    converter: SliderConverter<T>,
    onValueChangeFinished: (T) -> Unit,
    modifier: Modifier = Modifier,
    onValueChange: ((T) -> Unit)? = null,
    enabled: Boolean = true,
    trackHeight: Dp = Theme.size.box3,
    trackColor: Color = Theme.color.backgroundVariant,
    activeColor: Color = Theme.color.primaryContainer,
    trackShape: Shape = Theme.shape.v7,
    showThumb: Boolean = true,
    thumbColor: Color = Theme.color.onContainer,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    Slider(
        value = remember(converter, value) { converter.from(value) },
        onValueChangeFinished = { onValueChangeFinished(converter.to(it)) },
        modifier = modifier,
        onValueChange = { onValueChange?.invoke(converter.to(it)) },
        enabled = enabled,
        trackHeight = trackHeight,
        trackColor = trackColor,
        activeColor = activeColor,
        trackShape = trackShape,
        showThumb = showThumb,
        thumbColor = thumbColor,
        content = content
    )
}