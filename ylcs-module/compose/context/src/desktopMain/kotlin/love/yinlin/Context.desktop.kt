package love.yinlin

import androidx.compose.runtime.Stable
import androidx.compose.ui.awt.ComposeWindow

@Stable
actual class Context actual constructor(delegate: PlatformContextDelegate) {
    // 在 initDelay 后可用
    lateinit var window: ComposeWindow
    val handle: Long get() = window.windowHandle

    fun bindWindow(window: ComposeWindow) {
        this.window = window
    }
}