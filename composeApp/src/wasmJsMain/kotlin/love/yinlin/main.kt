package love.yinlin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import love.yinlin.extension.DateEx
import love.yinlin.platform.ActualAppContext
import love.yinlin.platform.AppContext
import love.yinlin.platform.app

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    app = ActualAppContext().initialize()

    try {
        ComposeViewport(document.body!!) {
            AppWrapper {
                App()
            }
        }
    }
    catch (e: Throwable) {
        val error = e.stackTraceToString()
        app.kv.set(AppContext.CRASH_KEY, "${DateEx.CurrentString}\n$error")
        println(error)
    }
}