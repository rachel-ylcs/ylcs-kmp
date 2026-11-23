package love.yinlin.compose.screen

import androidx.compose.runtime.Composable

@Composable
internal expect fun ScreenNavigationHost(manager: ScreenManager, content: @Composable () -> Unit)
