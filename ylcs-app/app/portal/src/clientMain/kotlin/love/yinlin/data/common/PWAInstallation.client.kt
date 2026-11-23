package love.yinlin.data.common

import androidx.compose.runtime.Stable

@Stable
internal actual object PWAInstallation {
    actual val shouldSuggest: Boolean get() = false
    actual val installAction: (() -> Unit)? get() = null
}
