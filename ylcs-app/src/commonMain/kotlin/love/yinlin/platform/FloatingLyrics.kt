package love.yinlin.platform

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.DeviceWrapper
import love.yinlin.common.Device

@Stable
abstract class FloatingLyrics {
    protected var currentLyrics: String? by mutableStateOf(null)

    abstract val canAttached: Boolean
    abstract val isAttached: Boolean
    abstract fun applyPermission(onResult: (Boolean) -> Unit)
    abstract fun attach()
    abstract fun detach()
    abstract fun updateLyrics(lyrics: String?)

    @Composable
    fun ContentWrapper(content: @Composable () -> Unit) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            DeviceWrapper(
                device = remember(this.maxWidth) { Device(this.maxWidth) },
                themeMode = app.config.themeMode,
                fontScale = 1f,
                content = content
            )
        }
    }

    @Composable
    abstract fun FloatingContent()
}