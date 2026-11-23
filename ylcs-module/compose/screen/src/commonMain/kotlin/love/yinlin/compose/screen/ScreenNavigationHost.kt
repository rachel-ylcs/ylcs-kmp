package love.yinlin.compose.screen

import androidx.compose.runtime.Composable

@Composable
internal expect inline fun ScreenNavigationHost(manager: ScreenManager, crossinline content: @Composable () -> Unit)
