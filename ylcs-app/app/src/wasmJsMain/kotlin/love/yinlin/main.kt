package love.yinlin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import love.yinlin.compose.PlatformContext
import love.yinlin.platform.ActualAppContext
import love.yinlin.platform.app

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    app1 = AppContext1(PlatformContext)
    ActualAppContext().apply {
        app = this
        initialize()
    }
    ComposeViewport(document.body!!) {
        AppEntry {
            ScreenEntry()
        }
    }
}