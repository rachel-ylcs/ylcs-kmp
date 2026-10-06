package love.yinlin

import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

fun main() {
    ComposeViewport(viewportContainer = document.body!!) {
        App()
    }
}