package love.yinlin.compose.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import love.yinlin.compose.Device
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.compose.ui.floating.FloatingArgsSheet
import love.yinlin.compose.ui.floating.FloatingDialog

@Stable
abstract class SubScreen(val parent: BasicScreen) {
    val slot: ScreenSlot get() = parent.slot

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

    open suspend fun initialize() {}
	open suspend fun update() {}

    @Composable
    abstract fun Content(device: Device)

    open val fabIcon: ImageVector? get() = null
	open val fabCanExpand: Boolean get() = false
	open val fabMenus: Array<FABAction> = emptyArray()
	open suspend fun onFabClick() {}

	@Composable
	open fun Floating() {}

	@Composable
	fun ComposedFloating() {
		// Sheet Land
		for (instance in landSheets) instance.Land()

		// Dialog Land
		for (instance in landDialogs) instance.Land()

		// Custom Floating Land
		Floating()
	}

    fun launch(block: suspend CoroutineScope.() -> Unit): Job = parent.launch(block = block)

	inline fun <reified S : BasicScreen> navigate(s: (ScreenManager) -> S) =
		parent.navigate(s)

	inline fun <reified S : BasicScreen, reified A1> navigate(s: (ScreenManager, A1) -> S, arg1: A1) =
		parent.navigate(s, arg1)

	inline fun <reified S : BasicScreen, reified A1, reified A2> navigate(s: (ScreenManager, A1, A2) -> S, arg1: A1, arg2: A2) =
		parent.navigate(s, arg1, arg2)

	inline fun <reified S : BasicScreen, reified A1, reified A2, reified A3> navigate(s: (ScreenManager, A1, A2, A3) -> S, arg1: A1, arg2: A2, arg3: A3) =
		parent.navigate(s, arg1, arg2, arg3)

    fun pop() = parent.pop()

	fun <T> monitor(state: () -> T, action: suspend (T) -> Unit) = parent.monitor(state, action)
}