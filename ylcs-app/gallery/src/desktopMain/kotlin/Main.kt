import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.v2.WindowBoundsProvider
import androidx.compose.ui.window.v2.WindowSizeProvider
import androidx.compose.ui.window.v2.WindowState
import androidx.compose.ui.window.v2.singleWindowApplication
import love.yinlin.App
import love.yinlin.compose.image.ImageManager
import love.yinlin.foundation.PlatformContext

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ImageManager.initialize(PlatformContext.Instance, "RachelGallery")
    singleWindowApplication(
        state = WindowState(initialBoundsProvider = WindowBoundsProvider(
            sizeProvider = WindowSizeProvider.Fixed(1280.dp, 768.dp)
        )),
        title = "Rachel UI Gallery"
    ) {
        App()
    }
}
