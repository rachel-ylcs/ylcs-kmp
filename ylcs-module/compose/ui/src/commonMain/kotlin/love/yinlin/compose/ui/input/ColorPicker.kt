package love.yinlin.compose.ui.input

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.graphics.HSV
import love.yinlin.compose.graphics.hsv
import love.yinlin.compose.ui.layout.MeasurePolicies
import kotlin.math.min

@Composable
private fun ValueSlider(
    hsv: HSV,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    val brush = remember(hsv) { Brush.verticalGradient(colors = listOf(hsv.copy(value = 1f).color, Colors.Black)) }

    Layout(modifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDrag = { change, _ -> onValueChange(1 - (change.position.y / size.height).coerceIn(0f, 1f)) },
            onDragEnd = onFinished
        )
    }.pointerInput(Unit) {
        detectTapGestures { offset ->
            onValueChange(1 - (offset.y / size.height).coerceIn(0f, 1f))
            onFinished()
        }
    }.drawBehind {
        val (w, h) = size
        val height = h * 0.05f

        drawRect(brush)
        drawRoundRect(
            color = Colors.White,
            topLeft = Offset(0f, ((1 - hsv.value) * h - height / 2).coerceIn(0f, h - height)),
            size = Size(w, height),
            cornerRadius = CornerRadius(height / 4),
            style = Stroke(width = height / 4)
        )
    }, measurePolicy = MeasurePolicies.Empty)
}

@Composable
private fun SpectrumArea(
    hsv: HSV,
    onHsvChange: (Float, Float) -> Unit,
    onFinished: () -> Unit
) {
    val hueBrush = remember {
        Brush.horizontalGradient(colors = List(7) { i -> HSV(i * 60f, 1f, 1f).color })
    }
    val saturationBrush = remember {
        Brush.verticalGradient(colors = listOf(Colors.Transparent, Colors.White))
    }

    Layout(modifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDrag = { change, _ ->
                onHsvChange((change.position.x / size.width).coerceIn(0f, 1f) * 360, 1 - (change.position.y / size.height).coerceIn(0f, 1f))
            },
            onDragEnd = onFinished
        )
    }.pointerInput(Unit) {
        detectTapGestures { offset ->
            val h = (offset.x / size.width).coerceIn(0f, 1f) * 360f
            val s = 1f - (offset.y / size.height).coerceIn(0f, 1f)
            onHsvChange(h, s)
            onFinished()
        }
    }.drawBehind {
        clipRect {
            drawRect(brush = hueBrush)
            drawRect(brush = saturationBrush)

            val x = (hsv.hue / 360f) * size.width
            val y = (1f - hsv.saturation) * size.height
            val boxSize = min(size.width, size.height) * 0.05f
            val center = Offset(x, y)
            val stroke = Stroke(width = boxSize * 0.25f)

            drawCircle(color = Colors.Black, radius = boxSize * 1.1f, center = center, style = stroke)
            drawCircle(color = Colors.White, radius = boxSize, center = center, style = stroke)
        }
    }, measurePolicy = MeasurePolicies.Empty)
}

@Composable
private fun AlphaSlider(
    color: Color,
    alpha: Float,
    enabled: Boolean,
    onAlphaChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    val brush = remember(color) { Brush.verticalGradient(colors = listOf(color.copy(alpha = 1f), Colors.Transparent)) }
    val disabledContainer = Theme.color.disabledContainer

    Layout(modifier = Modifier.pointerInput(enabled) {
        if (enabled) {
            detectDragGestures(
                onDrag = { change, _ -> onAlphaChange(1 - (change.position.y / size.height).coerceIn(0f, 1f)) },
                onDragEnd = onFinished
            )
        }
    }.pointerInput(enabled) {
        if (enabled) {
            detectTapGestures { offset ->
                onAlphaChange(1 - (offset.y / size.height).coerceIn(0f, 1f))
                onFinished()
            }
        }
    }.drawBehind {
        val (w, h) = size
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
            topLeft = Offset(0f, ((1 - alpha) * h - height / 2).coerceIn(0f, h - height)),
            size = Size(w, height),
            cornerRadius = CornerRadius(height / 4),
            style = Stroke(width = height / 4)
        )
    }, measurePolicy = MeasurePolicies.Empty)
}

@Composable
fun ColorPicker(
    onColorChangeFinished: (Color) -> Unit,
    modifier: Modifier = Modifier,
    initColor: Color = Theme.color.primary,
    enableAlpha: Boolean = true,
    onColorChanged: ((Color) -> Unit)? = null,
) {
    var currentHsv by rememberState { initColor.hsv }
    var currentAlpha by rememberState { initColor.alpha }

    Layout(
        modifier = modifier.defaultMinSize(minWidth = 200.dp, minHeight = 150.dp).border(Theme.border.v10, Theme.color.outline),
        content = {
            ValueSlider(
                hsv = currentHsv,
                onValueChange = {
                    val newHsv = currentHsv.copy(value = it)
                    currentHsv = newHsv
                    onColorChanged?.invoke(newHsv.color.copy(alpha = currentAlpha))
                },
                onFinished = { onColorChangeFinished(currentHsv.color.copy(alpha = currentAlpha)) }
            )

            SpectrumArea(
                hsv = currentHsv,
                onHsvChange = { h, s ->
                    val newHsv = currentHsv.copy(hue = h, saturation = s)
                    currentHsv = newHsv
                    onColorChanged?.invoke(newHsv.color.copy(alpha = currentAlpha))
                },
                onFinished = { onColorChangeFinished(currentHsv.color.copy(alpha = currentAlpha)) }
            )

            AlphaSlider(
                color = currentHsv.color,
                alpha = currentAlpha,
                enabled = enableAlpha,
                onAlphaChange = { alpha ->
                    currentAlpha = alpha
                    onColorChanged?.invoke(currentHsv.color.copy(alpha = alpha))
                },
                onFinished = { onColorChangeFinished(currentHsv.color.copy(alpha = currentAlpha)) }
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