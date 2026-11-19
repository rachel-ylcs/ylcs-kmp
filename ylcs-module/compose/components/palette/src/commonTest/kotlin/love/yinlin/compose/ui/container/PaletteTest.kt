package love.yinlin.compose.ui.container

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PaletteTest {
    @Test
    fun convertsRgbToHsl() {
        val hsl = PaletteColorUtils.rgbToHsl(0xFFFF0000.toInt())

        assertEquals(0f, hsl[0], 0.001f)
        assertEquals(1f, hsl[1], 0.001f)
        assertEquals(0.5f, hsl[2], 0.001f)
    }

    @Test
    fun quantizerPreservesPixelPopulation() {
        val pixels = IntArray(100) { if (it < 70) 0xFFE02020.toInt() else 0xFF2040E0.toInt() }
        val swatches = ColorCutQuantizer(pixels, 4, emptyList()).quantize()

        assertEquals(2, swatches.size)
        assertEquals(100, swatches.sumOf { it.population })
        assertEquals(70, swatches.maxOf { it.population })
    }

    @Test
    fun selectsSwatchForVibrantTarget() {
        val vibrant = Palette.Swatch(0xFFE02020.toInt(), 20)
        val muted = Palette.Swatch(0xFF777777.toInt(), 80)
        val palette = Palette([vibrant, muted], [Target.VIBRANT])

        assertEquals(vibrant, palette.vibrantSwatch)
        assertNotNull(palette.dominantSwatch)
        assertEquals(muted, palette.dominantSwatch)
    }

    @Test
    fun targetBuilderCopiesPreset() {
        val custom = Target.Builder(Target.VIBRANT)
            .setTargetLightness(0.6f)
            .setExclusive(false)
            .build()

        assertEquals(Target.VIBRANT.minimumSaturation, custom.minimumSaturation)
        assertEquals(0.6f, custom.targetLightness)
        assertEquals(false, custom.isExclusive)
    }
}
