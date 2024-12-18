package love.yinlin.platform

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

actual object Platform {
    actual val platformName: String = "Java ${System.getProperty("java.version")}"
    actual val isPortrait: Boolean = false
    actual val designDensity: Int = 480
    actual val designWidth: Dp = 360.dp
    actual val designHeight: Dp = 800.dp
}