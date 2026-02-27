package love.yinlin.media.lyrics

import androidx.compose.runtime.*
import love.yinlin.foundation.Context

@Stable
actual class FloatingLyrics {
    actual var isAttached: Boolean by mutableStateOf(false)
        private set

    actual fun attach() {}

    actual fun detach() {}

    actual suspend fun initDelay(context: Context) {}

    actual fun update() {}

    @Composable
    actual fun Content() {

    }
}