package love.yinlin.compose.graphics

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class HSVTest {
    @Test
    fun packsAndUnpacksComponents() {
        val hsv = HSV(217.25f, 0.63f, 0.84f)
        val restored = HSV(hsv.packedValue)

        assertEquals(hsv, restored)
        assertEquals(217.25f, restored.hue, 0.0001f)
        assertEquals(0.63f, restored.saturation, 0.000001f)
        assertEquals(0.84f, restored.value, 0.000001f)
        assertEquals(hsv, hsv.copy())
        assertEquals(hsv.hue, hsv.component1())
        assertEquals(hsv.saturation, hsv.component2())
        assertEquals(hsv.value, hsv.component3())
    }

    @Test
    fun keepsHueEndpointDistinct() {
        val start = HSV(0f, 1f, 1f)
        val end = HSV(360f, 1f, 1f)

        assertNotEquals(start.packedValue, end.packedValue)
        assertEquals(1f, end.color.red, 0.000001f)
        assertEquals(0f, end.color.green, 0.000001f)
        assertEquals(0f, end.color.blue, 0.000001f)
    }

    @Test
    fun convertsColorsToAndFromHsv() {
        val colors = [
            Color(1f, 0f, 0f),
            Color(0f, 1f, 0f),
            Color(0f, 0f, 1f),
            Color(0.23f, 0.48f, 0.81f),
        ]

        for (color in colors) {
            val restored = color.hsv.color
            assertEquals(color.red, restored.red, 0.00001f)
            assertEquals(color.green, restored.green, 0.00001f)
            assertEquals(color.blue, restored.blue, 0.00001f)
        }
    }

    @Test
    fun rejectsComponentsOutsideHsvRanges() {
        assertFailsWith<IllegalArgumentException> { HSV(361f, 1f, 1f) }
        assertFailsWith<IllegalArgumentException> { HSV(0f, -0.1f, 1f) }
        assertFailsWith<IllegalArgumentException> { HSV(0f, 1f, 1.1f) }
    }
}
