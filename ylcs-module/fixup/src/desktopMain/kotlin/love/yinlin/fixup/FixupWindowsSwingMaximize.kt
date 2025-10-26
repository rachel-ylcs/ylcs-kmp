package love.yinlin.fixup

import androidx.compose.ui.awt.ComposeWindow
import love.yinlin.platform.Platform
import java.awt.Rectangle

// See https://github.com/JetBrains/compose-multiplatform/issues/1724
data object FixupWindowsSwingMaximize {
    fun setBounds(window: ComposeWindow) {
        Platform.use(Platform.Windows) {
            val screenBounds = window.graphicsConfiguration.bounds
            val screenInsets = window.toolkit.getScreenInsets(window.graphicsConfiguration)
            window.maximizedBounds = Rectangle(
                screenBounds.x + screenInsets.left,
                screenBounds.y + screenInsets.top,
                screenBounds.width - screenInsets.left - screenInsets.right,
                screenBounds.height - screenInsets.top - screenInsets.bottom
            )
        }
    }
}