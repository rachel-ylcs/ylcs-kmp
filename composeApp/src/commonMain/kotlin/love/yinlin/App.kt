package love.yinlin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
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
import love.yinlin.common.RachelTheme
import love.yinlin.extension.LaunchOnce
import love.yinlin.extension.launchFlag
import love.yinlin.platform.app
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.screen.buildRoute
import love.yinlin.ui.screen.ScreenMain
import love.yinlin.ui.screen.community.ScreenPartDiscovery
import love.yinlin.ui.screen.community.ScreenPartMe
import love.yinlin.ui.screen.msg.ScreenPartMsg
import love.yinlin.ui.screen.music.ScreenPartMusic
import love.yinlin.ui.screen.world.ScreenPartWorld

@Stable
abstract class ScreenPart(private val model: AppModel) {
	private val firstLoad = launchFlag()

	fun launch(block: suspend CoroutineScope.() -> Unit): Job = model.launch(block = block)
	fun navigate(route: Screen<*>, options: NavOptions? = null, extras: Navigator.Extras? = null) = model.navigate(route, options, extras)

	@Composable
	protected abstract fun content()

	protected open suspend fun initialize() {}

	@Composable
	fun partContent() {
		content()
		LaunchOnce(firstLoad) {
			launch { initialize() }
		}
	}
}

class AppModel(
	private val navController: NavController
) : ViewModel() {
	val worldPart = ScreenPartWorld(this)
	val msgPart = ScreenPartMsg(this)
	val musicPart = ScreenPartMusic(this)
	val discoveryPart = ScreenPartDiscovery(this)
	val mePart = ScreenPartMe(this)

	inline fun <reified P : ScreenPart> part(): P = when (P::class) {
		ScreenPartWorld::class -> worldPart as P
		ScreenPartMsg::class -> msgPart as P
		ScreenPartMusic::class -> musicPart as P
		ScreenPartDiscovery::class -> discoveryPart as P
		ScreenPartMe::class -> mePart as P
		else -> error("unknown model part")
	}

	fun launch(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch(block = block)
	fun navigate(route: Screen<*>, options: NavOptions? = null, extras: Navigator.Extras? = null) = navController.navigate(route, options, extras)
	fun pop() {
		if (navController.previousBackStackEntry != null) navController.popBackStack()
	}
}

@Composable
fun App(modifier: Modifier = Modifier.fillMaxSize()) {
	val navController = rememberNavController()
	val appModel = viewModel { AppModel(navController) }

	NavHost(
		modifier = modifier.background(MaterialTheme.colorScheme.background),
		navController = navController,
		startDestination = ScreenMain,
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