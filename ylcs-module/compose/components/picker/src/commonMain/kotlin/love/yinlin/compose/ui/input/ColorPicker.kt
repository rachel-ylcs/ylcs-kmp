package love.yinlin.compose.ui.input

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.graphics.HSV
import love.yinlin.compose.ui.layout.MeasurePolicies

@Composable
private fun ValueSlider(
    hsv: HSV,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    val brush = remember(hsv) { Brush.verticalGradient(colors = [HSV(hsv.hue, hsv.saturation, 1f).color, Colors.Black]) }
    val onValueChangeUpdate by rememberUpdatedState(onValueChange)
    val onFinishedUpdate by rememberUpdatedState(onFinished)

    Layout(
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, _ -> onValueChangeUpdate(1 - (change.position.y / size.height).fastCoerceIn(0f, 1f)) },
                onDragEnd = onFinishedUpdate
            )
        }.pointerInput(Unit) {
            detectTapGestures { offset ->
                onValueChangeUpdate(1 - (offset.y / size.height).fastCoerceIn(0f, 1f))
                onFinishedUpdate()
            }
        }.drawWithContent {
            val [w, h] = size
            val height = h * 0.05f

            drawRect(brush)
            drawRoundRect(
                color = Colors.White,
                topLeft = Offset(0f, ((1 - hsv.value) * h - height / 2).fastCoerceIn(0f, h - height)),
                size = Size(w, height),
                cornerRadius = CornerRadius(height / 4),
                style = Stroke(width = height / 4)
            )
        },
        measurePolicy = MeasurePolicies.Empty
    )
}

private val HueBrush = Brush.horizontalGradient(colors = List(7) { i -> HSV(i * 60f, 1f, 1f).color })
private val SaturationBrush = Brush.verticalGradient(colors = [Colors.Transparent, Colors.White])

@Composable
private fun SpectrumArea(
    hsv: HSV,
    onHsvChange: (Float, Float) -> Unit,
    onFinished: () -> Unit
) {
    val onHsvChangeUpdate by rememberUpdatedState(onHsvChange)
    val onFinishedUpdate by rememberUpdatedState(onFinished)

    Layout(
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, _ ->
                    onHsvChangeUpdate((change.position.x / size.width).fastCoerceIn(0f, 1f) * 360, 1 - (change.position.y / size.height).fastCoerceIn(0f, 1f))
                },
                onDragEnd = onFinishedUpdate
            )
        }.pointerInput(Unit) {
            detectTapGestures { offset ->
                val h = (offset.x / size.width).fastCoerceIn(0f, 1f) * 360f
                val s = 1f - (offset.y / size.height).fastCoerceIn(0f, 1f)
                onHsvChangeUpdate(h, s)
                onFinishedUpdate()
            }
        }.drawWithContent {
            clipRect {
                drawRect(brush = HueBrush)
                drawRect(brush = SaturationBrush)

                val x = (hsv.hue / 360f) * size.width
                val y = (1f - hsv.saturation) * size.height
                val boxSize = minOf(size.width, size.height) * 0.05f
                val center = Offset(x, y)
                val stroke = Stroke(width = boxSize * 0.25f)

                drawCircle(color = Colors.Black, radius = boxSize * 1.1f, center = center, style = stroke)
                drawCircle(color = Colors.White, radius = boxSize, center = center, style = stroke)
            }
        },
        measurePolicy = MeasurePolicies.Empty
    )
}

@Composable
private fun AlphaSlider(
    color: Color,
    alpha: Float,
    enabled: Boolean,
    onAlphaChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    val brush = remember(color) { Brush.verticalGradient(colors = [color.copy(alpha = 1f), Colors.Transparent]) }
    val disabledContainer = Theme.color.disabledContainer
    val onAlphaChangeUpdate by rememberUpdatedState(onAlphaChange)
    val onFinishedUpdate by rememberUpdatedState(onFinished)

    Layout(
        modifier = Modifier.pointerInput(enabled) {
            if (enabled) {
                detectDragGestures(
                    onDrag = { change, _ -> onAlphaChangeUpdate(1 - (change.position.y / size.height).fastCoerceIn(0f, 1f)) },
                    onDragEnd = onFinishedUpdate
                )
            }
        }.pointerInput(enabled) {
            if (enabled) {
                detectTapGestures { offset ->
                    onAlphaChangeUpdate(1 - (offset.y / size.height).fastCoerceIn(0f, 1f))
                    onFinishedUpdate()
                }
            }
        }.drawWithContent {
            val [w, h] = size
            val height = h * 0.05f
            val tipSize = height / 2
            val tipBounds = Size(tipSize, tipSize)

            for (y in 0 ..< (h / tipSize).toInt()) {
                for (x in 0 ..< (w / tipSize).toInt()) {
                    if ((x + y) % 2 == 0) drawRect(color = Colors.Gray5, topLeft = Offset(x * tipSize, y * tipSize), size = tipBounds)
                }
            }

            drawRect(brush)

            if (!enabled) drawRect(disabledContainer)
            else drawRoundRect(
                color = Colors.White,
                topLeft = Offset(0f, ((1 - alpha) * h - height / 2).fastCoerceIn(0f, h - height)),
                size = Size(w, height),
                cornerRadius = CornerRadius(height / 4),
                style = Stroke(width = height / 4)
            )
        },
        measurePolicy = MeasurePolicies.Empty
    )
}

@Composable
fun ColorPicker(
    onColorChangeFinished: (Color) -> Unit,
    modifier: Modifier = Modifier,
    initColor: Color = Theme.color.primary,
    enableAlpha: Boolean = true,
    onColorChanged: ((Color) -> Unit)? = null,
) {
    var currentHsv by rememberValueState(HSV(initColor))
    var currentAlpha by rememberState { initColor.alpha }
    val onColorChangeFinishedUpdate by rememberUpdatedState(onColorChangeFinished)
    val onColorChangedUpdate by rememberUpdatedState(onColorChanged)

    Layout(
        modifier = modifier.defaultMinSize(minWidth = 200.dp, minHeight = 150.dp).border(Theme.border.v10, Theme.color.outline),
        content = {
            ValueSlider(
                hsv = currentHsv,
                onValueChange = {
                    val newHsv = HSV(currentHsv.hue, currentHsv.saturation, it)
                    currentHsv = newHsv
                    onColorChangedUpdate?.invoke(newHsv.color.copy(alpha = currentAlpha))
                },
                onFinished = { onColorChangeFinishedUpdate(currentHsv.color.copy(alpha = currentAlpha)) }
            )

            SpectrumArea(
                hsv = currentHsv,
                onHsvChange = { h, s ->
                    val newHsv = HSV(h, s, currentHsv.value)
                    currentHsv = newHsv
                    onColorChangedUpdate?.invoke(newHsv.color.copy(alpha = currentAlpha))
                },
                onFinished = { onColorChangeFinishedUpdate(currentHsv.color.copy(alpha = currentAlpha)) }
            )

            AlphaSlider(
                color = currentHsv.color,
                alpha = currentAlpha,
                enabled = enableAlpha,
                onAlphaChange = { alpha ->
                    currentAlpha = alpha
                    onColorChangedUpdate?.invoke(currentHsv.color.copy(alpha = alpha))
                },
                onFinished = { onColorChangeFinishedUpdate(currentHsv.color.copy(alpha = currentAlpha)) }
            )
        }
    ) { measurables, constraints ->
        val totalWidth = if (constraints.hasFixedWidth) constraints.maxWidth else constraints.minWidth
        val totalHeight = if (constraints.hasFixedHeight) constraints.maxHeight else constraints.minHeight

        val contentWidth = (totalWidth / 1.25f).toInt()
        val sliderWidth = contentWidth / 8

        val valuePlaceable = measurables[0].measure(Constraints.fixed(sliderWidth, totalHeight))
        val contentPlaceable = measurables[1].measure(Constraints.fixed(contentWidth, totalHeight))
        val alphaPlaceable = measurables[2].measure(Constraints.fixed(sliderWidth, totalHeight))

        layout(totalWidth, totalHeight) {
            valuePlaceable.place(0, 0)
            contentPlaceable.place(sliderWidth, 0)
            alphaPlaceable.place(sliderWidth + contentWidth, 0)
        }
    }
}