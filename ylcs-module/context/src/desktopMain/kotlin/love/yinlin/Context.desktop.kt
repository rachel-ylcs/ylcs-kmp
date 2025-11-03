package love.yinlin

import androidx.compose.ui.awt.ComposeWindow

actual class Context actual constructor(delegate: PlatformContextDelegate) {
    lateinit var window: ComposeWindow

    val handle: Long get() = window.windowHandle

    fun bindWindow(window: ComposeWindow) {
        this.window = window
    }
}