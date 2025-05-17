package love.yinlin

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import love.yinlin.common.*
import love.yinlin.extension.launchFlag
import love.yinlin.platform.app
import love.yinlin.ui.screen.*
import love.yinlin.ui.screen.common.ScreenMain
import love.yinlin.ui.screen.community.ScreenPartDiscovery
import love.yinlin.ui.screen.community.ScreenPartMe
import love.yinlin.ui.screen.msg.ScreenPartMsg
import love.yinlin.ui.screen.music.ScreenPartMusic
import love.yinlin.ui.screen.world.ScreenPartWorld

@Stable
abstract class ScreenPart(val model: AppModel) {
	val firstLoad = launchFlag()

	val slot: SubScreenSlot get() = model.slot

	fun launch(block: suspend CoroutineScope.() -> Unit): Job = model.launch(block = block)
	inline fun <reified A : Any> navigate(route: A, options: NavOptions? = null, extras: Navigator.Extras? = null) = model.navigate(route, options, extras)
	inline fun <reified T : Screen<Unit>> navigate(options: NavOptions? = null, extras: Navigator.Extras? = null) = model.navigate<T>(options, extras)
	fun deeplink(uri: Uri) = model.deeplink.process(uri)

	fun <T> monitor(state: () -> T, action: suspend (T) -> Unit) {
		launch { snapshotFlow(state).collectLatest(action) }
	}

	open suspend fun initialize() {}
	open suspend fun update() {}

	@Composable
	abstract fun Content()

	@Composable
	open fun Floating() {}
}

@Stable
class AppModel(
	val navController: NavController
) : ViewModel() {
	val deeplink = DeepLink(this)

	val worldPart = ScreenPartWorld(this)
	val msgPart = ScreenPartMsg(this)
	val musicPart = ScreenPartMusic(this)
	val discoveryPart = ScreenPartDiscovery(this)
	val mePart = ScreenPartMe(this)
	val parts = arrayOf(worldPart, msgPart, musicPart, discoveryPart, mePart)

	val slot = SubScreenSlot(viewModelScope)

	fun launch(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch(block = block)
	inline fun <reified A : Any> navigate(route: A, options: NavOptions? = null, extras: Navigator.Extras? = null) = navController.navigate(route, options, extras)
	inline fun <reified T : Screen<Unit>> navigate(options: NavOptions? = null, extras: Navigator.Extras? = null) = navController.navigate(route<T>(), options, extras)
	fun pop() {
		if (navController.previousBackStackEntry != null) navController.popBackStack()
	}
}

@Composable
fun App(
	navController: NavHostController = rememberNavController(),
	modifier: Modifier = Modifier.fillMaxSize()
) {
	val appModel: AppModel = viewModel {
		AppModel(navController).apply {
			app.model = this
		}
	}
	NavHost(
		modifier = modifier.background(MaterialTheme.colorScheme.background),
		navController = navController,
		startDestination = route<ScreenMain>(),
		enterTransition = {
			slideIntoContainer(
				towards = AnimatedContentTransitionScope.SlideDirection.Start,
				animationSpec = tween(
					durationMillis = app.config.animationSpeed,
					easing = FastOutSlowInEasing
				)
			)
		},
		exitTransition = {
			slideOutOfContainer(
				towards = AnimatedContentTransitionScope.SlideDirection.End,
				animationSpec = tween(
					durationMillis = app.config.animationSpeed,
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

@Composable
fun DeviceWrapper(
	device: Device,
	themeMode: ThemeMode,
	fontScale: Float,
	content: @Composable () -> Unit
) {
	val isDarkMode = when (themeMode) {
		ThemeMode.SYSTEM -> isSystemInDarkTheme()
		ThemeMode.LIGHT -> false
		ThemeMode.DARK -> true
	}

	CompositionLocalProvider(
		LocalDevice provides device,
		LocalDarkMode provides isDarkMode
	) {
		MaterialTheme(
			colorScheme = rachelColorScheme(isDarkMode),
			shapes = rachelShapes(device),
			typography = rachelTypography(device)
		) {
			CompositionLocalProvider(
				LocalContentColor provides MaterialTheme.colorScheme.onBackground,
				LocalTextStyle provides MaterialTheme.typography.bodyMedium,
				LocalDensity provides Density(LocalDensity.current.density, fontScale)
			) {
				content()
			}
		}
	}
}

@Composable
fun AppWrapper(content: @Composable () -> Unit) {
	BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
		DeviceWrapper(
			device = remember(maxWidth, maxHeight) { Device(maxWidth, maxHeight) },
			themeMode = app.config.themeMode,
			fontScale = app.config.fontScale,
			content = content
		)
	}
}