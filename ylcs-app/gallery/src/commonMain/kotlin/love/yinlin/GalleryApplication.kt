package love.yinlin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.compose.ColorSystem
import love.yinlin.compose.PlatformApplication
import love.yinlin.compose.ThemeMode
import love.yinlin.compose.ToolingTheme
import love.yinlin.compose.image.ImageManager
import love.yinlin.extension.BaseLazyReference
import love.yinlin.foundation.PlatformContext
import love.yinlin.gallery.resources.Res
import love.yinlin.gallery.resources.font

@Stable
abstract class GalleryApplication<out A : GalleryApplication<A>>(self: BaseLazyReference<A>, context: PlatformContext) : PlatformApplication<A>(self, context) {
    override val themeMode: ThemeMode get() = if (darkMode) ThemeMode.DARK else ThemeMode.LIGHT
    override val mainFontResource = Res.font.font
    override val colorSystem: ColorSystem get() = if (galleryPalette == GalleryPalette.Framework) ColorSystem.Default else GalleryColors
    override val toolingTheme = ToolingTheme(enableBallonTip = true)

    init {
        sync { ImageManager.initialize(rawContext, "RachelGallery") }
    }

    @Composable
    final override fun Content() = App()
}
