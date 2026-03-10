package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import love.yinlin.extension.BaseLazyReference
import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.uri.Uri
import kotlin.js.ExperimentalWasmJsInterop

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    @Composable
    protected open fun BeginContent() {}

    protected open val enableAccessibility: Boolean = true

    @OptIn(ExperimentalComposeUiApi::class)
    fun run() {
        val mainScope = MainScope()
        openService(scope = mainScope)
        mainScope.launch { openServiceLater() }

        // 有待考证
        window.onclose = {
            closeServiceBefore()
            closeService()
            mainScope.cancel()
        }

        ComposeViewport(
            viewportContainer = document.body!!,
            configure = {
                isA11YEnabled = enableAccessibility
            }
        ) {
            ComposedLayout {
                BeginContent()
                Content()
            }
        }
    }

    actual fun openUri(uri: Uri): Boolean {
        window.open(uri.toString(), "_blank")
        return true
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    actual fun copyText(text: String): Boolean {
        window.navigator.clipboard.writeText(text)
        return true
    }
}