@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.cancel
import love.yinlin.extension.BaseLazyReference
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingError
import love.yinlin.extension.cleaning
import love.yinlin.foundation.PlatformContext
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.Uri
import love.yinlin.uri.WebFileUri
import org.w3c.dom.COMPLETE
import org.w3c.dom.DocumentReadyState

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    context: PlatformContext,
) : Application<A>(self, context) {
    constructor(self: BaseLazyReference<A>) : this(self, PlatformContext.Instance)

    @Composable
    protected open fun BeginContent() {}

    protected open val enableAccessibility: Boolean = true

    fun run() {
        val mainScope = MainScope()
        val lifecycle = WebPageLifecycle(
            window,
            onReady = { initPoolLater(mainScope) },
            onClose = {
                cleaning(::destroyPoolBefore) {
                    cleaning(::destroyPool, mainScope::cancel)
                }
            }
        )

        catchingError {
            initApplication(scope = mainScope)
            lifecycle.bind(document.readyState == DocumentReadyState.COMPLETE)
            ComposeViewport(configure = { isA11YEnabled = enableAccessibility }) {
                ComposedLayout {
                    BeginContent()
                    Content()
                }
            }
        }?.let { error ->
            catchingError { lifecycle.close() }?.let { if (it !== error) error.addSuppressed(it) }
            throw error
        }
    }

    actual fun backHome() { }

    actual suspend fun openUri(uri: String): Boolean = catchingDefault(false) {
        window.open(uri, "_blank") != null
    }

    actual suspend fun copyText(text: String): Boolean = catchingDefault(false) {
        window.navigator.clipboard.writeText(text).await()
        true
    }

    actual fun implicitFileUri(uri: Uri): ImplicitUri = WebFileUri.resolve(uri)
}
