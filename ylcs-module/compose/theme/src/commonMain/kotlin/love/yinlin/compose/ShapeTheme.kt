package love.yinlin.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Stable
data class ShapeTheme(
    val v1: RoundedCornerShape,
    val v2: RoundedCornerShape,
    val v3: RoundedCornerShape,
    val v4: RoundedCornerShape,
    val v5: RoundedCornerShape,
    val v6: RoundedCornerShape,
    val v7: RoundedCornerShape,
    val v8: RoundedCornerShape,
    val v9: RoundedCornerShape,
    val v10: RoundedCornerShape,
) {
    companion object {
        private val Circle = RoundedCornerShape(50)
        private val Rectangle = object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Rectangle(size.toRect())
        }

        private val Number.shape: RoundedCornerShape get() = RoundedCornerShape(this.toDouble().dp)

        val Default = ShapeTheme(
            v1 = 20.shape,
            v2 = 18.shape,
            v3 = 16.shape,
            v4 = 14.shape,
            v5 = 12.shape,
            v6 = 10.shape,
            v7 = 8.shape,
            v8 = 6.shape,
            v9 = 4.shape,
            v10 = 2.shape,
        )
    }

    val circle: Shape = Circle
    val rectangle: Shape = Rectangle
}