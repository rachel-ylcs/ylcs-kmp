package love.yinlin.compose.graphics

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

@Stable
data class HSV(
    val hue: Float, // 0 - 360
    val saturation: Float, // 0 - 1
    val value: Float, // 0 - 1
) {
    val color: Color get() {
        val c = value * saturation
        val x = c * (1 - abs((hue / 60) % 2 - 1))
        val m = value - c

        var r = 0f
        var g = 0f
        var b = 0f

        when {
            hue < 60f -> { r = c; g = x }
            hue < 120f -> { r = x; g = c }
            hue < 180f -> { g = c; b = x }
            hue < 240f -> { g = x; b = c }
            hue < 300f -> { r = x; b = c }
            else -> { r = c; b = x }
        }

        return Color(r + m, g + m, b + m)
    }
}

val Color.hsv: HSV get() {
    val r = red
    val g = green
    val b = blue
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
        value = max
    )
}