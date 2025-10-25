package love.yinlin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import love.yinlin.platform.ActualAppContext
import love.yinlin.platform.app
import love.yinlin.service.PlatformContext

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    service.init(PlatformContext)
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