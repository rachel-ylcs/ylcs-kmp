package love.yinlin.compose.game

import androidx.collection.lruCache
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType

@Stable
class TextDrawer(
    private val font: FontFamily,
    private val fontFamilyResolver: FontFamily.Resolver
) {
    @Stable
    class TextCache(maxSize: Int = 8) {
        @Stable
        private data class CacheKey(
            val text: String,
            val height: Float,
            val fontWeight: FontWeight
        )

        private val lruCache = lruCache<CacheKey, Paragraph>(maxSize)

        fun measureText(manager: TextDrawer, text: String, height: Float, fontWeight: FontWeight = FontWeight.Light): Paragraph {
            val cacheKey = CacheKey(text, height, fontWeight)
            val cacheResult = lruCache[cacheKey]
            if (cacheResult != null) return cacheResult
            val newResult = manager.makeParagraph(text, height, fontWeight)
            lruCache.put(cacheKey, newResult)
            return newResult
        }
    }

    private val density = Density(1f)

    fun makeParagraph(text: String, height: Float, fontWeight: FontWeight = FontWeight.Light): Paragraph {
        // 查询缓存
        val intrinsics = ParagraphIntrinsics(
            text = text,
            style = TextStyle(
                fontSize = TextUnit(height / 1.17f, TextUnitType.Sp),
                fontWeight = fontWeight,
                fontFamily = font
            ),
            annotations = emptyList(),
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            placeholders = emptyList()
        )
        return Paragraph(
            paragraphIntrinsics = intrinsics,
            constraints = Constraints.fitPrioritizingWidth(minWidth = 0, maxWidth = intrinsics.maxIntrinsicWidth.toInt(), minHeight = 0, maxHeight = height.toInt()),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }

    fun DrawScope.text(
        content: Paragraph,
        position: Offset,
        color: Color,
        shadow: Shadow? = null,
        decoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) {
        withTransform({
            translate(left = position.x, top = position.y)
            clipRect(left = 0f, top = 0f, right = content.width, bottom = content.height)
        }) {
            content.paint(
                canvas = drawContext.canvas,
                color = color,
                shadow = shadow,
                textDecoration = decoration,
                drawStyle = drawStyle,
                blendMode = blendMode
            )
        }
    }

    fun DrawScope.text(
        content: Paragraph,
        position: Offset,
        brush: Brush,
        shadow: Shadow? = null,
        decoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) {
        withTransform({
            translate(left = position.x, top = position.y)
            clipRect(left = 0f, top = 0f, right = content.width, bottom = content.height)
        }) {
            content.paint(
                canvas = drawContext.canvas,
                brush = brush,
                shadow = shadow,
                textDecoration = decoration,
                drawStyle = drawStyle,
                blendMode = blendMode
            )
        }
    }
}