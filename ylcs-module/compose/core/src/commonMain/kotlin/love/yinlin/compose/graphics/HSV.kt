package love.yinlin.compose.graphics

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.roundToLong

@Stable
@JvmInline
value class HSV(val packedValue: Long) {
    constructor(hue: Float, saturation: Float, value: Float) : this(pack(hue, saturation, value))

    val hue: Float get() = unpack(HUE_SHIFT, HUE_MASK, HUE_MAX) * 360f
    val saturation: Float get() = unpack(SATURATION_SHIFT, CHANNEL_MASK, CHANNEL_MAX)
    val value: Float get() = unpack(VALUE_SHIFT, CHANNEL_MASK, CHANNEL_MAX)

    val color: Color get() {
        val hue = hue
        val saturation = saturation
        val value = value
        val c = value * saturation
        val x = c * (1 - abs((hue / 60f) % 2f - 1))
        val m = value - c

        var r = 0f
        var g = 0f
        var b = 0f

        when {
            hue < 60f -> {
                r = c
                g = x
            }
            hue < 120f -> {
                r = x
                g = c
            }
            hue < 180f -> {
                g = c
                b = x
            }
            hue < 240f -> {
                g = x
                b = c
            }
            hue < 300f -> {
                r = x
                b = c
            }
            else -> {
                r = c
                b = x
            }
        }

        return Color(r + m, g + m, b + m)
    }

    private fun unpack(shift: Int, mask: Long, maximum: Int): Float = ((packedValue ushr shift) and mask).toFloat() / maximum

    fun copy(hue: Float = this.hue, saturation: Float = this.saturation, value: Float = this.value): HSV = HSV(hue, saturation, value)

    operator fun component1(): Float = hue
    operator fun component2(): Float = saturation
    operator fun component3(): Float = value

    override fun toString(): String = "HSV(hue=$hue, saturation=$saturation, value=$value)"

    companion object {
        private const val HUE_SHIFT = 42
        private const val SATURATION_SHIFT = 21
        private const val VALUE_SHIFT = 0
        private const val HUE_MAX = 0x3FFFFF
        private const val CHANNEL_MAX = 0x1FFFFF
        private const val HUE_MASK = 0x3FFFFFL
        private const val CHANNEL_MASK = 0x1FFFFFL

        private fun pack(hue: Float, saturation: Float, value: Float): Long {
            require(hue in 0f .. 360f) { "Hue must be in the range 0..360." }
            require(saturation in 0f .. 1f) { "Saturation must be in the range 0..1." }
            require(value in 0f .. 1f) { "Value must be in the range 0..1." }

            val hueBits = (hue / 360f * HUE_MAX).roundToLong()
            val saturationBits = (saturation * CHANNEL_MAX).roundToLong()
            val valueBits = (value * CHANNEL_MAX).roundToLong()

            return (hueBits shl HUE_SHIFT) or (saturationBits shl SATURATION_SHIFT) or valueBits
        }

        operator fun invoke(color: Color): HSV {
            val r = color.red
            val g = color.green
            val b = color.blue
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min

            val h = when {
                delta == 0f -> 0f
                max == r -> ((g - b) / delta) % 6
                max == g -> ((b - r) / delta) + 2
                else -> ((r - g) / delta) + 4
            } * 60

            return HSV(hue = if (h < 0) h + 360 else h, saturation = if (max == 0f) 0f else delta / max, value = max)
        }
    }
}
