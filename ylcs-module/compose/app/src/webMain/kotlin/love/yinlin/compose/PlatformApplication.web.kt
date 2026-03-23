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
import love.yinlin.foundation.PlatformContext
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.RegularUri
import love.yinlin.uri.Uri
import kotlin.js.ExperimentalWasmJsInterop

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    context: PlatformContext,
) : Application<A>(self, context) {
    constructor(self: BaseLazyReference<A>): this(self, PlatformContext.Instance)

    @Composable
    protected open fun BeginContent() {}

    protected open val enableAccessibility: Boolean = true

    @OptIn(ExperimentalComposeUiApi::class)
    fun run() {
        val mainScope = MainScope()
        initApplicationService(scope = mainScope)
        mainScope.launch { initServiceLater() }

        // 有待考证
        window.onclose = {
            destroyServiceBefore()
            destroyService()
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

    actual fun backHome() { }

    actual fun openUri(uri: Uri): Boolean {
        window.open(uri.toString(), "_blank")
        return true
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    actual fun copyText(text: String): Boolean {
        window.navigator.clipboard.writeText(text)
        return true
    }

    actual fun implicitFileUri(uri: Uri): ImplicitUri = RegularUri(uri.toString())
}