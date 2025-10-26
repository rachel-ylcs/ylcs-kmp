package love.yinlin.fixup

import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.core.view.WindowInsetsControllerCompat

// See https://stackoverflow.com/questions/38382283/change-status-bar-text-color-when-primarydark-is-white/66197063#66197063
@Stable
data object FixupAndroidStatusBarColor {
    @Composable
    fun AutoTheme(window: Window, isDarkMode: Boolean) {
        LaunchedEffect(window, isDarkMode) {
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
        }
    }
}