package love.yinlin.compose.game.drawer

import androidx.compose.runtime.Stable
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.text.font.FontFamily
import love.yinlin.compose.game.asset.AssetProvider
import love.yinlin.compose.game.font.FontProvider

@Stable
open class PrepareDrawer internal constructor(
    fontFamilyResolver: FontFamily.Resolver,
    fontProvider: FontProvider,
    assetProvider: AssetProvider
) : InitialDrawer(fontFamilyResolver, fontProvider, assetProvider) {
    private var rawCacheScope: CacheDrawScope? = null

    internal inline fun withRawCacheScope(scope: CacheDrawScope, block: PrepareDrawer.() -> Unit) {
        rawCacheScope = scope
        density = scope
        block()
        density = null
        rawCacheScope = null
    }
}