package love.yinlin

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication

fun main() = singleWindowApplication(
    state = WindowState(width = 1280.dp, height = 768.dp),
    title = "Rachel UI Gallery"
) {
    App()
}