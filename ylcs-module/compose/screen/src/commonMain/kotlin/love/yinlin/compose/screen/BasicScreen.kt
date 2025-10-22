package love.yinlin.compose.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.rememberImmersivePadding
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.compose.ui.floating.FABLayout

@Stable
abstract class BasicScreen<A>(val manager: ScreenManager) : ViewModel() {
    init {
        manager.loadViewModel(this)
    }

    open suspend fun initialize() {}

    open fun finalize() {}

    @Composable
    protected abstract fun BasicContent()

    protected open val fabIcon: ImageVector? get() = null
    protected open val fabCanExpand: Boolean get() = false
    protected open val fabMenus: Array<FABAction> = emptyArray()
    protected open suspend fun onFabClick() {}

    @Composable
    protected open fun Floating() {}

    val slot = ScreenSlot(viewModelScope)

    @Composable
    fun ComposedUI() {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            BasicContent()
        }

        val immersivePadding = rememberImmersivePadding()
        CompositionLocalProvider(LocalImmersivePadding provides immersivePadding) {
            fabIcon?.let {
                FABLayout(
                    icon = it,
                    canExpand = fabCanExpand,
                    onClick = ::onFabClick,
                    menus = fabMenus
                )
            }

            Floating()

            with(slot) {
                info.Land()
                confirm.Land()
                loading.Land()
                tip.Land()
            }
        }
    }

    final override fun onCleared() {
        super.onCleared()
        manager.unloadViewModel(this)
        finalize()
    }

    fun launch(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch(block = block)
    inline fun <reified T : Any> navigate(route: T, options: NavOptions? = null, extras: Navigator.Extras? = null) = manager.navigate(route, options, extras)
    inline fun <reified T : BasicScreen<Unit>> navigate(options: NavOptions? = null, extras: Navigator.Extras? = null) = manager.navigate<T>(options, extras)
    fun pop() = manager.pop()
    fun <T> monitor(state: () -> T, action: suspend (T) -> Unit) = launch { snapshotFlow(state).collectLatest(action) }

    // 深层链接
}

typealias CommonBasicScreen = BasicScreen<Unit>