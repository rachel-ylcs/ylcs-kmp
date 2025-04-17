package love.yinlin.ui.screen

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.ui.component.screen.SubScreenSlot
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KType

@Stable
abstract class Screen<A : Screen.Args>(val model: AppModel) : ViewModel() {
	@Stable
	interface Args

	fun launch(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch(block = block)
	fun navigate(route: Args, options: NavOptions? = null, extras: Navigator.Extras? = null) = model.navigate(route, options, extras)
	fun pop() = model.pop()

	inline fun <reified P : ScreenPart> part(): P = model.part()

	val slot = SubScreenSlot(viewModelScope)

	open suspend fun initialize() {}

	@Composable
	abstract fun content()
}

data class ScreenRouteScope(
	val builder: NavGraphBuilder,
	val model: AppModel
)

inline fun <reified A : Screen.Args> ScreenRouteScope.screen(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    crossinline factory: (AppModel, A) -> Screen<A>
) {
	val appModel = this.model
	this.builder.composable<A>(typeMap = typeMap) {  backStackEntry ->
		val screen = viewModel {
			factory(appModel, backStackEntry.toRoute<A>()).also {
				it.launch { it.initialize() }
			}
		}
		screen.content()
	}
}

inline fun <reified A : Screen.Args> ScreenRouteScope.screen(
	typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
	crossinline factory: (AppModel) -> Screen<A>
) {
	val appModel = this.model
	this.builder.composable<A>(typeMap = typeMap) {  backStackEntry ->
		val screen = viewModel {
			factory(appModel).also {
				it.launch { it.initialize() }
			}
		}
		screen.content()
	}
}