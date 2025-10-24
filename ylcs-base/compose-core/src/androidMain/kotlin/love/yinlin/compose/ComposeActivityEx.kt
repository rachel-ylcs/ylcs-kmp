package love.yinlin.compose

import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowInsetsControllerCompat

fun ComponentActivity.enabledImmersiveMode() {
    enableEdgeToEdge()
    window.isNavigationBarContrastEnforced = false
}

@Composable
fun autoStatusBarTheme(window: Window, isDarkMode: Boolean) {
    LaunchedEffect(window, isDarkMode) {
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
    }
}