package love.yinlin.compose.screen

import androidx.compose.runtime.Composable

@Composable
internal actual inline fun ScreenNavigationHost(manager: ScreenManager, crossinline content: @Composable () -> Unit) = content()
