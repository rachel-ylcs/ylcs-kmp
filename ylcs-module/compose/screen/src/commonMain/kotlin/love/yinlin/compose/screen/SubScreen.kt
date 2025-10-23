package love.yinlin.compose.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import love.yinlin.compose.Device
import love.yinlin.compose.LaunchFlag
import love.yinlin.compose.ui.floating.FABAction

@Stable
abstract class SubScreen(
    val parent: BasicScreen<*>,

) {
    val firstLoad = LaunchFlag()

    val slot: ScreenSlot get() = parent.slot

    open suspend fun initialize(update: Boolean) {}

    @Composable
    abstract fun Content(device: Device)

    open val fabIcon: ImageVector? get() = null
	open val fabCanExpand: Boolean get() = false
	open val fabMenus: Array<FABAction> = emptyArray()
	open suspend fun onFabClick() {}

	@Composable
	open fun Floating() {}

    fun launch(block: suspend CoroutineScope.() -> Unit): Job = parent.launch(block = block)
	inline fun <reified A : Any> navigate(route: A, options: NavOptions? = null, extras: Navigator.Extras? = null) = parent.navigate(route, options, extras)
	inline fun <reified T : Screen<Unit>> navigate(options: NavOptions? = null, extras: Navigator.Extras? = null) = parent.navigate<T>(options, extras)
    fun pop() = parent.pop()
	fun <T> monitor(state: () -> T, action: suspend (T) -> Unit) = parent.monitor(state, action)
}