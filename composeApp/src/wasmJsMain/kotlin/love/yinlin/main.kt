package love.yinlin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import love.yinlin.platform.AppContext
import love.yinlin.platform.appContext

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    appContext = AppContext().initialize()
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