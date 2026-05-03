package love.yinlin.compose.game.drawer

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density

@Stable
actual class StrokeTextGraph internal actual constructor(
    text: String,
    font: FontFamily?,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    fontWeight: FontWeight,
    fontStyle: FontStyle,
    letterSpacing: Float,
    actual override val shadow: Shadow?,
    actual override val textDecoration: TextDecoration?,
    actual override val blendMode: BlendMode,
) : BasicTextGraph {
    private val paragraph = BasicTextGraph.measureText(text, font, density, fontFamilyResolver, fontWeight, fontStyle, letterSpacing)
    private val strokeParagraph = BasicTextGraph.measureText(text, font, density, fontFamilyResolver, fontWeight, fontStyle, letterSpacing)

    actual override fun widthScale(height: Float): Long = BasicTextGraph.widthScale(paragraph, height)
    actual override fun width(height: Float): Float = BasicTextGraph.width(paragraph, height)
    actual override fun height(width: Float): Float = BasicTextGraph.height(paragraph, width)
    actual override fun size(height: Float): Size = BasicTextGraph.size(paragraph, height)
    actual fun paint(canvas: Canvas, color: Color, strokeColor: Color, stroke: Stroke) {
        paragraph.paint(canvas, color, shadow, textDecoration, null, blendMode)
        strokeParagraph.paint(canvas, strokeColor, null, textDecoration, stroke, blendMode)
    }
    actual fun paint(canvas: Canvas, brush: Brush, alpha: Float, strokeColor: Color, stroke: Stroke) {
        paragraph.paint(canvas, brush, alpha, shadow, textDecoration, null, blendMode)
        strokeParagraph.paint(canvas, strokeColor.copy(alpha = alpha), null, textDecoration, stroke, blendMode)
    }
}