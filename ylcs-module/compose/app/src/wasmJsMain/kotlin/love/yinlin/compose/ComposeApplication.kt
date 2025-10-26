package love.yinlin.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun composeApplication(
    entry: @Composable (framework: @Composable () -> Unit) -> Unit = { framework -> App { framework() } },
    content: @Composable BoxScope.() -> Unit
) {
    ComposeViewport(document.body!!) {
        entry {
            Box(
                modifier = Modifier.fillMaxSize(),
                content = content
            )
        }
    }
}