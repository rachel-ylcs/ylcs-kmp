package love.yinlin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.v2.WindowBoundsProvider
import androidx.compose.ui.window.v2.WindowSizeProvider
import androidx.compose.ui.window.v2.WindowState
import androidx.compose.ui.window.v2.singleWindowApplication

@OptIn(ExperimentalComposeUiApi::class)
fun main() = singleWindowApplication(
    state = WindowState(initialBoundsProvider = WindowBoundsProvider(
        sizeProvider = WindowSizeProvider.Fixed(1280.dp, 768.dp)
    )),
    title = "Rachel UI Gallery"
) {
    App()
}