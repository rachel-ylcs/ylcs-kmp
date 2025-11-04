package love.yinlin.fixup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import kotlinx.coroutines.delay
import love.yinlin.platform.Coroutines
import love.yinlin.platform.Platform
import java.awt.Dimension
import java.awt.Rectangle

@Stable
actual data object Fixup {
    // See https://youtrack.jetbrains.com/issue/CMP-2285/Min-Max-size-window
    @Composable
    fun swingWindowMinimize(window: ComposeWindow, minSize: DpSize) {
        LaunchedEffect(LocalDensity.current) {
            window.minimumSize = Dimension(minSize.width.value.toInt(), minSize.height.value.toInt())
        }
    }

    // See https://github.com/JetBrains/compose-multiplatform/issues/1724
    fun swingWindowMaximizeBounds(window: ComposeWindow) {
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

    // ignoresMouseEvents on macOS is buggy
    // See https://stackoverflow.com/questions/29441015
    inline fun macOSClickEventDelay(value: Boolean, crossinline setValue: (Boolean) -> Unit) {
        Platform.use(Platform.MacOS) {
            if (value) {
                setValue(false)
                Coroutines.startMain {
                    delay(100)
                    setValue(true)
                }
            }
        }
    }
}