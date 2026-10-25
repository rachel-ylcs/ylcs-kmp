package love.yinlin.nativecompose.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals

/** Pure Graphics behavior; raster and text backend integration is covered by UiSkikoTest. */
class UiGraphicsTest {
    @Test
    fun colorsPreserveArgbAndComponents() {
        val color = Color(128, 64, 32, 128)
        assertEquals(0x80804020.toInt(), color.toArgb())
        assertEquals(128f / 255f, color.red, 0.00001f)
        assertEquals(128f / 255f, color.alpha, 0.00001f)
    }

    @Test
    fun transparentAndOpaqueCompositingPreserveDestinationAndSource() {
        assertEquals(Color.Blue, Color.Transparent.compositeOver(Color.Blue))
        assertEquals(Color.Red, Color.Red.compositeOver(Color.Blue))
    }

    @Test
    fun colorSpacesConvertWithoutLosingNeutralWhite() {
        val white = Color.White.convert(ColorSpaces.DisplayP3).convert(ColorSpaces.Srgb)
        assertEquals(1f, white.red, 0.001f)
        assertEquals(1f, white.green, 0.001f)
        assertEquals(1f, white.blue, 0.001f)
        assertEquals(1f, white.alpha)
    }

    @Test
    fun matricesMapTranslatedAndScaledCoordinates() {
        val matrix = Matrix()
        matrix.translate(10f, 20f)
        assertEquals(Offset(12f, 23f), matrix.map(Offset(2f, 3f)))
        matrix.reset()
        matrix.scale(2f, 3f)
        assertEquals(Offset(4f, 9f), matrix.map(Offset(2f, 3f)))
    }

    @Test
    fun colorMatricesRetainAlphaAndProduceNeutralSaturation() {
        val matrix = ColorMatrix()
        matrix.setToScale(2f, 3f, 4f, 0.5f)
        assertEquals(2f, matrix[0, 0])
        assertEquals(3f, matrix[1, 1])
        assertEquals(0.5f, matrix[3, 3])
        matrix.setToSaturation(0f)
        assertEquals(matrix[0, 0], matrix[1, 0])
        assertEquals(matrix[1, 0], matrix[2, 0])
        assertEquals(1f, matrix[3, 3])
    }

    @Test
    fun luminanceUsesLinearColorValues() {
        assertEquals(0f, Color.Black.luminance())
        assertEquals(1f, Color.White.luminance(), 0.00001f)
    }
}
