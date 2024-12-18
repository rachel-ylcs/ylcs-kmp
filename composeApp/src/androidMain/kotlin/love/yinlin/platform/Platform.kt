package love.yinlin.platform

import android.os.Build
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

actual object Platform {
    actual val platformName: String = "Android ${Build.VERSION.SDK_INT}"
    actual val isPortrait: Boolean = true
    actual val designDensity: Int = 480
    actual val designWidth: Dp = 360.dp
    actual val designHeight: Dp = 800.dp
}