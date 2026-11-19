/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package love.yinlin.compose.ui.container

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Stable
internal object PaletteColorUtils {
    fun rgbToHsl(rgb: Int): FloatArray = FloatArray(3).also { rgbToHsl(rgb, it) }

    fun rgbToHsl(rgb: Int, output: FloatArray) {
        val red = ((rgb shr 16) and 0xFF) / 255f
        val green = ((rgb shr 8) and 0xFF) / 255f
        val blue = (rgb and 0xFF) / 255f

        val maximum = max(red, max(green, blue))
        val minimum = min(red, min(green, blue))
        val delta = maximum - minimum
        val lightness = (maximum + minimum) / 2f

        if (delta == 0f) {
            output[0] = 0f
            output[1] = 0f
            output[2] = lightness
            return
        }

        val saturation = delta / (1f - abs(2f * lightness - 1f))
        val hue = when (maximum) {
            red -> 60f * (((green - blue) / delta) % 6f)
            green -> 60f * ((blue - red) / delta + 2f)
            else -> 60f * ((red - green) / delta + 4f)
        }

        output[0] = if (hue < 0f) hue + 360f else hue
        output[1] = saturation
        output[2] = lightness
    }

    fun toColor(argb: Int): Color = Color(
        red = ((argb shr 16) and 0xFF) / 255f,
        green = ((argb shr 8) and 0xFF) / 255f,
        blue = (argb and 0xFF) / 255f,
        alpha = ((argb ushr 24) and 0xFF) / 255f,
    )

    fun calculateMinimumAlpha(foreground: Int, background: Int, minimumContrast: Float): Int {
        if ((background ushr 24) != 0xFF) return -1
        if (calculateContrast(setAlpha(foreground, 255), background) < minimumContrast) return -1

        var minimum = 0
        var maximum = 255
        repeat(10) {
            if (maximum - minimum <= 1) return maximum

            val middle = (minimum + maximum) / 2
            val candidate = setAlpha(foreground, middle)
            if (calculateContrast(candidate, background) < minimumContrast) minimum = middle
            else maximum = middle
        }
        return maximum
    }

    private fun calculateContrast(foreground: Int, background: Int): Double {
        val composite = compositeColors(foreground, background)
        val foregroundLuminance = calculateLuminance(composite)
        val backgroundLuminance = calculateLuminance(background)
        val lighter = max(foregroundLuminance, backgroundLuminance)
        val darker = min(foregroundLuminance, backgroundLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun calculateLuminance(color: Int): Double {
        val red = linearize((color shr 16) and 0xFF)
        val green = linearize((color shr 8) and 0xFF)
        val blue = linearize(color and 0xFF)
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    private fun linearize(component: Int): Double {
        val value = component / 255.0
        return if (value < 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    private fun compositeColors(foreground: Int, background: Int): Int {
        val foregroundAlpha = (foreground ushr 24) and 0xFF
        val backgroundAlpha = (background ushr 24) and 0xFF
        val outputAlpha = 255 - ((255 - backgroundAlpha) * (255 - foregroundAlpha) / 255)

        if (outputAlpha == 0) return 0

        fun compositeComponent(foregroundShift: Int): Int {
            val foregroundComponent = (foreground shr foregroundShift) and 0xFF
            val backgroundComponent = (background shr foregroundShift) and 0xFF
            return (foregroundComponent * foregroundAlpha * 255 + backgroundComponent * backgroundAlpha * (255 - foregroundAlpha)) / (outputAlpha * 255)
        }

        return (outputAlpha shl 24) or (compositeComponent(16) shl 16) or (compositeComponent(8) shl 8) or compositeComponent(0)
    }

    private fun setAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)
}
