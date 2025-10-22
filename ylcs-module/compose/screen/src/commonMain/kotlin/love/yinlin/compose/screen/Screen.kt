package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Stable
abstract class Screen<A>(val manager: ScreenManager) : ViewModel() {
    open suspend fun initialize() {}
    open fun finalize() {}

    fun launch(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch(block = block)
    inline fun <reified T : Any> navigate(route: T, options: NavOptions? = null, extras: Navigator.Extras? = null) = manager.navigate(route, options, extras)
    inline fun <reified T : Screen<Unit>> navigate(options: NavOptions? = null, extras: Navigator.Extras? = null) = manager.navigate<T>(options, extras)
    fun pop() = manager.pop()
    fun <T> monitor(state: () -> T, action: suspend (T) -> Unit) = launch { snapshotFlow(state).collectLatest(action) }
}