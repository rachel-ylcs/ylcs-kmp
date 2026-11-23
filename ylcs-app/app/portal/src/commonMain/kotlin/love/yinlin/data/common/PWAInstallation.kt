package love.yinlin.data.common

import androidx.compose.runtime.Stable

@Stable
internal expect object PWAInstallation {
    val shouldSuggest: Boolean
    val installAction: (() -> Unit)?
}
