package love.yinlin.compose.game.drawer

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.packFloats

/**
 * Paragraph 在 android 和 skia 上有不一致的绘制表现
 * 在 android 上，Paragraph 居然维持状态不能够重复利用绘制
 */
@Stable
interface BasicTextGraph {
    val shadow: Shadow?
    val textDecoration: TextDecoration?
    val blendMode: BlendMode

    fun widthScale(height: Float): Long
    fun width(height: Float): Float
    fun size(height: Float): Size

    companion object {
        private val DefaultPlatformTextStyle = buildPlatformTextStyle()

        internal fun widthScale(paragraph: Paragraph, height: Float): Long {
            val rawHeight = paragraph.height
            val scale = if (rawHeight == 0f) 0f else height / rawHeight
            return packFloats(paragraph.width * scale, scale)
        }

        /**
         * 根据提供的高度计算宽度
         */
        internal fun width(paragraph: Paragraph, height: Float): Float {
            val rawHeight = paragraph.height
            return if (rawHeight == 0f) 0f else paragraph.width * height / rawHeight
        }

        /**
         * 根据提供的高度计算大小
         */
        internal fun size(paragraph: Paragraph, height: Float): Size {
            val rawHeight = paragraph.height
            return Size(if (rawHeight == 0f) 0f else paragraph.width * height / rawHeight, height)
        }

        internal val BaselineTextFontSize = 64.sp

        internal fun measureText(
            text: String,
            font: FontFamily?,
            density: Density,
            fontFamilyResolver: FontFamily.Resolver,
            fontWeight: FontWeight,
            fontStyle: FontStyle,
            letterSpacing: Float
        ): Paragraph {
            val intrinsics = ParagraphIntrinsics(
                text = text,
                style = TextStyle(
                    fontSize = BaselineTextFontSize,
                    fontWeight = fontWeight,
                    fontFamily = font,
                    fontStyle = fontStyle,
                    letterSpacing = if (letterSpacing <= 0f) TextUnit.Unspecified else BaselineTextFontSize * letterSpacing,
                    platformStyle = DefaultPlatformTextStyle,
                ),
                annotations = emptyList(),
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                placeholders = emptyList()
            )
            return Paragraph(
                paragraphIntrinsics = intrinsics,
                constraints = Constraints(maxWidth = intrinsics.maxIntrinsicWidth.toInt()),
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}