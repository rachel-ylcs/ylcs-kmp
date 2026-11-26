package love.yinlin.compose.graphics

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShaderProgramTest {
    @Test
    fun brushRetainsItsUniformsAfterAnotherBrushIsDrawn() {
        @OptIn(InternalComposeUiApi::class)
        registerSkikoComposeImplementation()
        val program = ShaderProgram(
            """
            uniform float red;
            uniform float2 resolution;

            half4 main(float2 position) {
                return half4(red, position.x / resolution.x, 0.0, 1.0);
            }
            """.trimIndent()
        )
        val first = program.brush {
            float("red", 1f)
            resolution("resolution")
        }
        val second = program.brush {
            float("red", 0f)
            resolution("resolution")
        }

        val original = draw(first, width = 2)
        assertEquals(0f, draw(second, width = 2).red)
        assertEquals(original, draw(first, width = 2))
        assertEquals(1f, original.red)
        assertTrue(draw(first, width = 4).green < original.green)
    }

    private fun draw(brush: ShaderBrush, width: Int): Color {
        val image = ImageBitmap(width, 2)
        val size = Size(width.toFloat(), 2f)
        val paint = Paint()
        brush.applyTo(size, paint, 1f)
        Canvas(image).drawRect(Rect(0f, 0f, size.width, size.height), paint)
        return image.toPixelMap()[0, 0]
    }
}
