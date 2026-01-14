package love.yinlin.platform.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.foundation.Context

@Stable
actual class FloatingLyrics {
    actual var isAttached: Boolean = false

    actual fun attach() { }

    actual fun detach() { }

    actual suspend fun initDelay(context: Context) { }

    actual fun update() { }

    @Composable
    actual fun Content() { }
}