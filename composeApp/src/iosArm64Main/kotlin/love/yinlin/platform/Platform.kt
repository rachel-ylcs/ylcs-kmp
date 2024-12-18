package love.yinlin.platform

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import platform.UIKit.UIDevice

actual object Platform {
    actual val platformName: String = "${UIDevice.Companion.currentDevice.systemName()} ${UIDevice.Companion.currentDevice.systemVersion}"
    actual val isPortrait: Boolean = true
    actual val designDensity: Int = 480
    actual val designWidth: Dp = 360.dp
    actual val designHeight: Dp = 800.dp
}