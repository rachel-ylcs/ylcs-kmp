package love.yinlin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import love.yinlin.ui.Route
import love.yinlin.ui.Route.Companion.buildRoute
import love.yinlin.ui.screen.MainModel

class AppModel(
	private val navController: NavController
) : ViewModel() {
	val mainModel = MainModel(this)

	fun <T : Any> navigate(route: T, options: NavOptions? = null, extras: Navigator.Extras? = null) = navController.navigate(route, options, extras)
	fun pop() = navController.popBackStack()
	fun launch(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch(block = block)
}

@Composable
fun App(modifier: Modifier = Modifier.fillMaxSize()) {
	val navController = rememberNavController()
	val appModel = viewModel { AppModel(navController) }
	NavHost(
		modifier = modifier.background(MaterialTheme.colorScheme.background),
		navController = navController,
		startDestination = Route.Main,
	) {
		buildRoute(appModel)
	}
}

@Composable
fun AppWrapper(content: @Composable () -> Unit) {
	CompositionLocalProvider(
		LocalDensity provides Density(
			density = app.screenWidth / app.designWidth.value,
			fontScale = app.fontScale
		)
	) {
		RachelTheme(app.isDarkMode) {
			Box(modifier = Modifier.fillMaxSize()) {
				content()
			}
		}
	}
}