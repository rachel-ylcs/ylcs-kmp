package love.yinlin.ui.component.input

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import love.yinlin.common.Colors
import love.yinlin.common.ThemeValue
import love.yinlin.extension.rememberState
import love.yinlin.ui.component.layout.Space
import kotlin.math.*

private data class HSV(val hue: Float, val saturation: Float, val brightness: Float)

private val Color.hsv: HSV get() {
    val r = this.red
    val g = this.green
    val b = this.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val h = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta) % 6
        max == g -> ((b - r) / delta) + 2
        else -> ((r - g) / delta) + 4
    } * 60
    return HSV(
        hue = if (h < 0) h + 360 else h,
        saturation = if (max == 0f) 0f else delta / max,
        brightness = max
    )
}

private fun Float.moveToLight(progress: Float): Int = (this + (255 - this) * progress).coerceIn(0f, 255f).roundToInt()

private fun getValidOffset(offset: Offset, radius: Float): Offset {
    val x = offset.x
    val y = offset.y
    val length = sqrt((x - radius) * (x - radius) + (y - radius) * (y - radius))
    return if (length > radius) {
        val angle = atan2(y - radius, x - radius)
        Offset(x - (length - radius) * cos(angle), y - (length - radius) * sin(angle))
    } else Offset(x, y)
}

private fun calculateColor(offset: Offset, radius: Float, alpha: Int): Color {
    val x = offset.x
    val y = offset.y
    val length = sqrt((x - radius) * (x - radius) + (y - radius) * (y - radius))
    val radiusProgress = 1 - (length / radius).coerceIn(0f, 1f)
    val angleProgress = (((atan2(y - radius, x - radius) * 180 / 3.141592f).roundToInt() + 360) % 360) / 360f
    return when {
        angleProgress < 1f / 6 -> Color(
            red = 255f.moveToLight(radiusProgress),
            green = (255f * angleProgress * 6).moveToLight(radiusProgress),
            blue = 0f.moveToLight(radiusProgress),
            alpha = alpha
        )
        angleProgress < 2f / 6 -> Color(
            red = (255 * (1 - (angleProgress * 6 - 1))).moveToLight(radiusProgress),
            green = 255f.moveToLight(radiusProgress),
            blue = 0f.moveToLight(radiusProgress),
            alpha = alpha
        )
        angleProgress < 3f / 6 -> Color(
            red = 0f.moveToLight(radiusProgress),
            green = 255f.moveToLight(radiusProgress),
            blue = (255 * (angleProgress * 6 - 2)).moveToLight(radiusProgress),
            alpha = alpha
        )
        angleProgress < 4f / 6 -> Color(
            red = 0f.moveToLight(radiusProgress),
            green = (255 * (1 - (angleProgress * 6 - 3))).moveToLight(radiusProgress),
            blue = 255f.moveToLight(radiusProgress),
            alpha = alpha
        )
        angleProgress < 5f / 6 -> Color(
            red = (255 * (angleProgress * 6 - 4)).moveToLight(radiusProgress),
            green = 0f.moveToLight(radiusProgress),
            blue = 255f.moveToLight(radiusProgress),
            alpha = alpha
        )
        else -> Color(
            red = 255f.moveToLight(radiusProgress),
            green = 0f.moveToLight(radiusProgress),
            blue = (255 * (1 - (angleProgress * 6 - 5))).moveToLight(radiusProgress),
            alpha = alpha
        )
    }
}

@Composable
fun DockedColorPicker(
    modifier: Modifier = Modifier,
    initialColor: Color = Colors.White,
    onColorChanged: ((Color) -> Unit)? = null,
    onColorChangeFinished: ((Color) -> Unit)? = null
) {
    Column(modifier = modifier) {
        var pickerColor by rememberState(initialColor) { initialColor }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val radius = with(LocalDensity.current) { (maxWidth / 2f).toPx() }
            var pickerLocation by rememberState(radius, initialColor) {
                val hsv = initialColor.hsv
                val angle = hsv.hue * 3.141592f / 180
                val saturation = hsv.saturation
                Offset(radius + cos(angle) * saturation * radius, radius + sin(angle) * saturation * radius)
            }

            Canvas(modifier = Modifier.fillMaxSize()
                .pointerInput(radius, initialColor, onColorChanged, onColorChangeFinished) {
                    detectDragGestures(onDragEnd = { onColorChangeFinished?.invoke(pickerColor) }) { change, _ ->
                        val validOffset = getValidOffset(change.position, radius)
                        pickerLocation = validOffset
                        pickerColor = calculateColor(validOffset, radius, (pickerColor.alpha * 255).roundToInt())
                        onColorChanged?.invoke(pickerColor)
                    }
                }.pointerInput(radius, initialColor, onColorChanged, onColorChangeFinished) {
                    detectTapGestures {
                        val validOffset = getValidOffset(it, radius)
                        pickerLocation = validOffset
                        pickerColor = calculateColor(validOffset, radius, (pickerColor.alpha * 255).roundToInt())
                        onColorChanged?.invoke(pickerColor)
                        onColorChangeFinished?.invoke(pickerColor)
                    }
                }
            ) {
                // Palette
                drawCircle(Brush.sweepGradient(listOf(Colors.Red, Colors.Yellow, Colors.Green, Colors.Cyan, Colors.Blue, Colors.Fuchsia, Colors.Red)))
                drawCircle(ShaderBrush(RadialGradientShader(
                    center = Offset(size.width / 2f, size.height / 2f),
                    colors = listOf(Color.White, Color.Transparent),
                    radius = size.width / 2f
                )))
                // Selector
                drawCircle(pickerColor, radius = radius / 4f, center = pickerLocation)
                drawCircle(Color.White, radius = radius / 4f, center = pickerLocation, style = Stroke(3f))
                drawCircle(Color.LightGray, radius = radius / 4f, center = pickerLocation, style = Stroke(1f))
            }
        }
        Space(size = ThemeValue.Padding.VerticalExtraSpace)
        ProgressSlider(
            value = pickerColor.alpha,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {
                pickerColor = pickerColor.copy(alpha = it)
                onColorChanged?.invoke(pickerColor)
            },
            onValueChangeFinished = {
                pickerColor = pickerColor.copy(alpha = it)
                onColorChangeFinished?.invoke(pickerColor)
            }
        )
    }
}