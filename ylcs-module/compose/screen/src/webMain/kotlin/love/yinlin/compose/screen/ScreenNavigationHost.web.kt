package love.yinlin.compose.screen

import androidx.compose.runtime.*
import love.yinlin.compose.extension.staticLocalComposition

private val LocalBrowserNavigationHost = staticLocalComposition { false }

@Composable
internal actual inline fun ScreenNavigationHost(manager: ScreenManager, crossinline content: @Composable () -> Unit) {
    if (LocalBrowserNavigationHost.current) content()
    else {
        val history = remember(manager) { BrowserScreenHistory(manager) }

        DisposableEffect(history) {
            history.attach()
            onDispose(history::detach)
        }

        LaunchedEffect(history) {
            snapshotFlow { manager.backStack.size > 1 }.collect { history.synchronize() }
        }

        CompositionLocalProvider(LocalBrowserNavigationHost provides true) {
            content()
        }
    }
}
