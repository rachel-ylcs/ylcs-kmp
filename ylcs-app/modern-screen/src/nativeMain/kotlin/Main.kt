import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.NativeWindow
import love.yinlin.App
import org.jetbrains.skiko.window.NativeWindowOptions

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    NativeWindow(NativeWindowOptions(title = "Rachel UI Gallery", width = 1280, height = 768)) {
        App()
    }
}