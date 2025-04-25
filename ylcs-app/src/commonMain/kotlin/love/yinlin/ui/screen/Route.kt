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
import love.yinlin.common.Uri
import love.yinlin.ui.component.screen.FloatingDialogConfirm
import love.yinlin.ui.component.screen.FloatingDialogInfo
import love.yinlin.ui.component.screen.FloatingDialogLoading
import love.yinlin.ui.component.screen.Tip
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KType

@Stable
class SubScreenSlot(scope: CoroutineScope) {
	val tip = Tip(scope)
	val info = FloatingDialogInfo()
	val confirm = FloatingDialogConfirm()
	val loading = FloatingDialogLoading()
}

@Stable
abstract class Screen<A : Screen.Args>(protected val model: AppModel) : ViewModel() {
	@Stable
	interface Args

	fun launch(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch(block = block)
	fun navigate(route: Args, options: NavOptions? = null, extras: Navigator.Extras? = null) = model.navigate(route, options, extras)
	fun pop() = model.pop()
	fun deeplink(uri: Uri) = model.deeplink.process(uri)

	val worldPart = model.worldPart
	val msgPart = model.msgPart
	val musicPart = model.musicPart
	val discoveryPart = model.discoveryPart
	val mePart = model.mePart

	val slot = SubScreenSlot(viewModelScope)

	open suspend fun initialize() {}

	@Composable
	protected abstract fun Content()

	@Composable
	protected open fun Floating() {}

	@Composable
	fun UI() {
		Content()
		Floating()

		with(slot) {
			info.Land()
			confirm.Land()
			loading.Land()
			tip.Land()
		}
	}
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
		screen.UI()
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
		screen.UI()
	}
}