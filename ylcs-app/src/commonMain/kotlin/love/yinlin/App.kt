package love.yinlin

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import love.yinlin.common.DeepLink
import love.yinlin.common.RachelTheme
import love.yinlin.common.Uri
import love.yinlin.extension.launchFlag
import love.yinlin.platform.app
import love.yinlin.ui.component.screen.SubScreenSlot
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.screen.ScreenMain
import love.yinlin.ui.screen.ScreenRouteScope
import love.yinlin.ui.screen.community.ScreenPartDiscovery
import love.yinlin.ui.screen.community.ScreenPartMe
import love.yinlin.ui.screen.msg.ScreenPartMsg
import love.yinlin.ui.screen.music.ScreenPartMusic
import love.yinlin.ui.screen.screens
import love.yinlin.ui.screen.world.ScreenPartWorld

@Stable
abstract class ScreenPart(private val model: AppModel) {
	val firstLoad = launchFlag()

	val slot: SubScreenSlot get() = model.slot

	fun launch(block: suspend CoroutineScope.() -> Unit): Job = model.launch(block = block)
	fun navigate(route: Screen.Args, options: NavOptions? = null, extras: Navigator.Extras? = null) = model.navigate(route, options, extras)
	fun deeplink(uri: Uri) = model.deeplink.process(uri)

	open suspend fun initialize() {}

	@Composable
	abstract fun Content()

	@Composable
	open fun Floating() {}
}

@Stable
class AppModel(
	private val navController: NavController
) : ViewModel() {
	val deeplink = DeepLink(this)

	val worldPart = ScreenPartWorld(this)
	val msgPart = ScreenPartMsg(this)
	val musicPart = ScreenPartMusic(this)
	val discoveryPart = ScreenPartDiscovery(this)
	val mePart = ScreenPartMe(this)

	val slot = SubScreenSlot(viewModelScope)

	fun launch(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch(block = block)
	fun navigate(route: Screen.Args, options: NavOptions? = null, extras: Navigator.Extras? = null) = navController.navigate(route, options, extras)
	fun pop() {
		if (navController.previousBackStackEntry != null) navController.popBackStack()
	}
}

@Composable
fun App(modifier: Modifier = Modifier.fillMaxSize()) {
	val navController = rememberNavController()
	val appModel = viewModel { AppModel(navController) }

	Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
		NavHost(
			modifier = Modifier.fillMaxSize(),
			navController = navController,
			startDestination = ScreenMain.Args,
			enterTransition = {
				slideIntoContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Start,
					animationSpec = tween(
						durationMillis = 300,
						easing = FastOutSlowInEasing
					)
				) + fadeIn(
					animationSpec = tween(
						durationMillis = 300,
						easing = FastOutSlowInEasing
					)
				)
			},
			exitTransition = {
				slideOutOfContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.End,
					animationSpec = tween(
						durationMillis = 300,
						easing = FastOutSlowInEasing
					)
				) + fadeOut(
					animationSpec = tween(
						durationMillis = 300,
						easing = FastOutSlowInEasing
					)
				)
			}
		) {
			with(ScreenRouteScope(this, appModel)) {
				screens()
			}
		}
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