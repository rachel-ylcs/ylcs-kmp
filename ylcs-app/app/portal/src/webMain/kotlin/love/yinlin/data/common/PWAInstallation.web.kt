@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.data.common

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.window

private fun shouldSuggestPwaInstall(): Boolean = js("window.rachelPwaInstall && window.rachelPwaInstall.shouldSuggest() || false")
private fun canPromptPwaInstall(): Boolean = js("window.rachelPwaInstall && window.rachelPwaInstall.canInstall() || false")
private fun promptPwaInstall(): Unit = js("window.rachelPwaInstall.prompt()")

@Stable
internal actual object PWAInstallation {
    private var canInstall by mutableStateOf(canPromptPwaInstall())

    init {
        window.addEventListener("rachel-pwa-install-change", { canInstall = canPromptPwaInstall() })
    }

    actual val shouldSuggest: Boolean get() = shouldSuggestPwaInstall()
    actual val installAction: (() -> Unit)? get() = if (canInstall) ::promptPwaInstall else null
}
