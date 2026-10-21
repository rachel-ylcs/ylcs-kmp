package love.yinlin

import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import love.yinlin.compose.image.ImageManager
import love.yinlin.foundation.PlatformContext

fun main() {
    ImageManager.initialize(PlatformContext.Instance, "RachelGallery")
    ComposeViewport(viewportContainer = document.body!!) {
        App()
    }
}
