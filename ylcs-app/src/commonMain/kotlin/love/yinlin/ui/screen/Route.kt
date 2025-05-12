package love.yinlin.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import love.yinlin.AppModel
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.Uri
import love.yinlin.common.rememberImmersivePadding
import love.yinlin.extension.getNavType
import love.yinlin.ui.component.screen.FloatingDialogConfirm
import love.yinlin.ui.component.screen.FloatingDialogInfo
import love.yinlin.ui.component.screen.FloatingDialogLoading
import love.yinlin.ui.component.screen.Tip
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Stable
class SubScreenSlot(scope: CoroutineScope) {
	val tip = Tip(scope)
	val info = FloatingDialogInfo()
	val confirm = FloatingDialogConfirm()
	val loading = FloatingDialogLoading()
}

@Stable
abstract class Screen<A>(val model: AppModel) : ViewModel() {
	fun launch(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch(block = block)
	inline fun <reified T : Any> navigate(route: T, options: NavOptions? = null, extras: Navigator.Extras? = null) = model.navigate(route, options, extras)
	inline fun <reified T : Screen<Unit>> navigate(options: NavOptions? = null, extras: Navigator.Extras? = null) = model.navigate<T>(options, extras)
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
		Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
			Content()
		}

		val immersivePadding = rememberImmersivePadding()
		CompositionLocalProvider(LocalImmersivePadding provides immersivePadding) {
			Floating()

			with(slot) {
				info.Land()
				confirm.Land()
				loading.Land()
				tip.Land()
			}
		}
	}
}

data class ScreenRouteScope(
	val builder: NavGraphBuilder,
	val model: AppModel
)

inline fun <reified S : Screen<Unit>> route(): String = "rachel.${S::class.qualifiedName!!}"

inline fun <reified S : Screen<Unit>> ScreenRouteScope.screen(crossinline factory: (AppModel) -> S) {
	val appModel = this.model
	this.builder.composable(route = route<S>()) {
		val screen = viewModel {
			factory(appModel).also {
				it.launch { it.initialize() }
			}
		}
		screen.UI()
	}
}

inline fun <reified A : Any> ScreenRouteScope.screen(
	crossinline factory: (AppModel, A) -> Screen<A>,
	typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap()
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

inline fun <reified T> ScreenRouteScope.type() = mapOf(typeOf<T>() to getNavType<T>())