package love.yinlin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.browser.document
import love.yinlin.model.AppModel
import love.yinlin.platform.KV
import love.yinlin.platform.WasmContext

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val context = WasmContext()
    val kv = KV()
    try {
        ComposeViewport(document.body!!) {
            AppWrapper(context) {
                App(viewModel { AppModel(kv) })
            }
        }
    }
    catch (e: Exception) {
        e.printStackTrace()
    }
}