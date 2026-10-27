import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.NativeWindow
import love.yinlin.App
import love.yinlin.compose.image.ImageManager
import love.yinlin.foundation.PlatformContext
import org.jetbrains.skiko.window.NativeWindowOptions

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ImageManager.initialize(PlatformContext.Instance, "RachelGallery")
    NativeWindow(NativeWindowOptions(title = "Rachel UI Gallery", width = 1280, height = 768)) {
        App()
    }
}
