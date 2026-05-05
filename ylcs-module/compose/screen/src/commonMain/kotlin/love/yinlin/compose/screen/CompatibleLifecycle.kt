package love.yinlin.compose.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavEntryDecorator

@Composable
fun <T : Any> rememberCompatibleViewModelStoreNavEntryDecorator(): ViewModelStoreNavEntryDecorator<T> {
    val owner = checkNotNull(LocalViewModelStoreOwner.current)
    // google 官方库在defaultExtras 和 defaultFactory 这里有bug
    val provider = remember(owner) { ViewModelStoreProvider(owner) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(provider, lifecycle) {
        onDispose {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) provider.clearAllKeys()
        }
    }

    return remember(owner, provider) { ViewModelStoreNavEntryDecorator(provider) }
}