package love.yinlin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import love.yinlin.platform.AppContext
import love.yinlin.platform.app

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    app = AppContext().initialize()

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