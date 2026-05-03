package love.yinlin.compose.game.drawer

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density

@Stable
class TextGraph internal constructor(
    text: String,
    font: FontFamily?,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    fontWeight: FontWeight,
    fontStyle: FontStyle,
    letterSpacing: Float,
    override val shadow: Shadow?,
    override val textDecoration: TextDecoration?,
    override val blendMode: BlendMode,
) : BasicTextGraph {
    private val paragraph = BasicTextGraph.measureText(text, font, density, fontFamilyResolver, fontWeight, fontStyle, letterSpacing)

    override fun widthScale(height: Float): Long = BasicTextGraph.widthScale(paragraph, height)
    override fun width(height: Float): Float = BasicTextGraph.width(paragraph, height)
    override fun height(width: Float): Float = BasicTextGraph.height(paragraph, width)
    override fun size(height: Float): Size = BasicTextGraph.size(paragraph, height)
    fun paint(canvas: Canvas, color: Color) = paragraph.paint(canvas, color, shadow, textDecoration, null, blendMode)
    fun paint(canvas: Canvas, brush: Brush, alpha: Float) = paragraph.paint(canvas, brush, alpha, shadow, textDecoration, null, blendMode)
}