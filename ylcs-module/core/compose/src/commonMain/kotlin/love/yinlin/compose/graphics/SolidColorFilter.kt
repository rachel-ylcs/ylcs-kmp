package love.yinlin.compose.graphics

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter

@Stable
object SolidColorFilter {
    operator fun invoke(color: Color): ColorMatrixColorFilter {
        val (r, g, b) = color
        return ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            0.21f * r, 0.71f * r, 0.07f * r, 0f, 0f,
            0.21f * g, 0.71f * g, 0.07f * g, 0f, 0f,
            0.21f * b, 0.71f * b, 0.07f * b, 0f, 0f,
            0f,        0f,        0f,        1f, 0f
        )))
    }
}