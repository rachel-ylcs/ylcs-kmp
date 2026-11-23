package love.yinlin.compose.screen

import androidx.compose.runtime.Composable

@Composable
internal actual fun ScreenNavigationHost(manager: ScreenManager, content: @Composable () -> Unit) = content()
