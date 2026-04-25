package love.yinlin.compose.game.drawer

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density

/**
 * compose 在 Paragraph 上对 Android 和 skia 有不一致的渲染
 * 处理描边+填充的文本在 Android 上必须分两次绘制，否则会覆盖掉 Paragraph 的结果
 * 参看 [androidx.compose.ui.text.Paragraph.paint]
 */
@Stable
expect class StrokeTextGraph internal constructor(
    text: String,
    font: FontFamily?,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    fontWeight: FontWeight,
    fontStyle: FontStyle,
    letterSpacing: Float,
    shadow: Shadow?,
    textDecoration: TextDecoration?,
    blendMode: BlendMode,
) : BasicTextGraph {
    override val shadow: Shadow?
    override val textDecoration: TextDecoration?
    override val blendMode: BlendMode
    override fun widthScale(height: Float): Long
    override fun width(height: Float): Float
    override fun height(width: Float): Float
    override fun size(height: Float): Size
    fun paint(canvas: Canvas, color: Color, strokeColor: Color, stroke: Stroke)
    fun paint(canvas: Canvas, brush: Brush, alpha: Float, strokeColor: Color, stroke: Stroke)
}