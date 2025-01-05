package love.yinlin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    appContext = WasmContext().initialize()
    try {
        ComposeViewport(document.body!!) {
            AppWrapper {
                App()
            }
        }
    }
    catch (e: Throwable) {
        e.printStackTrace()
    }
}