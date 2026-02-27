package love.yinlin.compose.game

import androidx.collection.lruCache
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType

@Stable
class TextDrawer(
    userFonts: List<FontFamily>,
    private val fontFamilyResolver: FontFamily.Resolver
) {
    @Stable
    class Cache(maxSize: Int = 8) {
        @Stable
        internal data class CacheKey(
            val text: String,
            val height: Float,
            val fontWeight: FontWeight,
            val fontStyle: FontStyle,
            val letterSpacingRatio: Float,
            val fontIndex: Int
        )

        private val lruCache = lruCache<CacheKey, Paragraph>(maxSize)

        internal fun measureText(
            manager: TextDrawer,
            text: String,
            height: Float,
            fontWeight: FontWeight,
            fontStyle: FontStyle,
            letterSpacing: Float,
            fontIndex: Int,
        ): Paragraph {
            // 查询缓存
            val cacheKey = CacheKey(text, height, fontWeight, fontStyle, letterSpacing, fontIndex)
            val cacheResult = lruCache[cacheKey]
            if (cacheResult != null) return cacheResult
            val newResult = manager.makeParagraph(cacheKey)
            lruCache.put(cacheKey, newResult)
            return newResult
        }
    }

    private val density = Density(1f)

    private val fonts: List<FontFamily> = buildList {
        if (userFonts.isEmpty()) add(FontFamily.Default)
        else addAll(userFonts)
    }

    private fun makeParagraph(cacheKey: Cache.CacheKey): Paragraph {
        val fontSize = TextUnit(cacheKey.height / 1.17f, TextUnitType.Sp)
        val intrinsics = ParagraphIntrinsics(
            text = cacheKey.text,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = cacheKey.fontWeight,
                fontFamily = fonts.getOrNull(cacheKey.fontIndex) ?: FontFamily.Default,
                fontStyle = cacheKey.fontStyle,
                lineHeight = fontSize * 1.5f,
                letterSpacing = fontSize * cacheKey.letterSpacingRatio,
            ),
            annotations = emptyList(),
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            placeholders = emptyList()
        )
        return Paragraph(
            paragraphIntrinsics = intrinsics,
            constraints = Constraints.fitPrioritizingWidth(minWidth = 0, maxWidth = intrinsics.maxIntrinsicWidth.toInt(), minHeight = 0, maxHeight = cacheKey.height.toInt()),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}