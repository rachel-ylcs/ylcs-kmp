package love.yinlin.compose.game.common

import androidx.collection.lruCache
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.font.FontFamily

@Stable
open class PrepareDrawer internal constructor(
    textCacheCapacity: Int,
    protected val fontFamilyResolver: FontFamily.Resolver,
    protected val fontProvider: FontProvider
) {
    private val textCache = lruCache<TextDrawCacheKey, Paragraph>(textCacheCapacity)
}