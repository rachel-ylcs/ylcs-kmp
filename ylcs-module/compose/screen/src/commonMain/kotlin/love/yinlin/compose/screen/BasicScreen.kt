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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.collection.StableList
import love.yinlin.compose.collection.emptyStableList
import love.yinlin.compose.rememberImmersivePadding
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.compose.ui.floating.FABLayout
import love.yinlin.compose.ui.floating.FloatingArgsSheet
import love.yinlin.compose.ui.floating.FloatingDialog

@Stable
abstract class BasicScreen(@PublishedApi internal val manager: ScreenManager) : ViewModel() {
    open suspend fun initialize() {}

    open fun finalize() {}

    @Composable
    protected abstract fun BasicContent()

    protected open val fabIcon: ImageVector? = null
    protected open val fabCanExpand: Boolean = false
    protected open val fabMenus: StableList<FABAction> = emptyStableList()
    protected open suspend fun onFabClick() {}

    @Composable
    protected open fun Floating() {}

    val slot = ScreenSlot(viewModelScope)

    private val landDialogs = mutableListOf<FloatingDialog<*>>()
    private val landSheets = mutableListOf<FloatingArgsSheet<*>>()

    protected infix fun <F : FloatingDialog<*>> land(instance: F): F {
        landDialogs += instance
        return instance
    }

    protected infix fun <F : FloatingArgsSheet<*>> land(instance: F): F {
        landSheets += instance
        return instance
    }

    @Composable
    fun ComposedUI() {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            BasicContent()
        }

        val immersivePadding = rememberImmersivePadding()
        CompositionLocalProvider(LocalImmersivePadding provides immersivePadding) {
            // FAB Layout
            fabIcon?.let {
                FABLayout(
                    icon = it,
                    canExpand = fabCanExpand,
                    onClick = ::onFabClick,
                    menus = fabMenus
                )
            }

            // Sheet Land
            for (instance in landSheets) instance.Land()

            // Dialog Land
            for (instance in landDialogs) instance.Land()

            // Custom Floating Land
            Floating()

            // Default Dialog Land
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
        manager.unregisterScreen(this)
        finalize()
    }

    fun launch(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch(block = block)

    inline fun <reified S : BasicScreen> navigate(s: (ScreenManager) -> S) =
        manager.navigate(s)

    inline fun <reified S : BasicScreen, reified A1> navigate(s: (ScreenManager, A1) -> S, arg1: A1) =
        manager.navigate(s, arg1)

    inline fun <reified S : BasicScreen, reified A1, reified A2> navigate(s: (ScreenManager, A1, A2) -> S, arg1: A1, arg2: A2) =
        manager.navigate(s, arg1, arg2)

    inline fun <reified S : BasicScreen, reified A1, reified A2, reified A3> navigate(s: (ScreenManager, A1, A2, A3) -> S, arg1: A1, arg2: A2, arg3: A3) =
        manager.navigate(s, arg1, arg2, arg3)

    fun pop() = manager.pop()

    fun <T> monitor(state: () -> T, action: suspend (T) -> Unit) = launch { snapshotFlow(state).collectLatest(action) }

    val Throwable?.warningTip: Throwable? get() {
        if (this != null) slot.tip.warning(this.message)
        return this
    }

    val Throwable?.errorTip: Throwable? get() {
        if (this != null) slot.tip.error(this.message)
        return this
    }
}