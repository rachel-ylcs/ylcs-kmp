package love.yinlin

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import love.yinlin.common.*
import love.yinlin.common.uri.Uri
import love.yinlin.compose.*
import love.yinlin.compose.screen.AppScreen
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.floating.localBalloonTipEnabled
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.xwwk
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.screen.common.ScreenMain
import love.yinlin.ui.screen.*
import love.yinlin.ui.screen.community.ScreenPartDiscovery
import love.yinlin.ui.screen.community.ScreenPartMe
import love.yinlin.ui.screen.msg.ScreenPartMsg
import love.yinlin.ui.screen.music.ScreenPartMusic
import love.yinlin.ui.screen.world.ScreenPartWorld

@Stable
abstract class ScreenPart(val model: AppModel) {
	val firstLoad = LaunchFlag()

	val slot: SubScreenSlot get() = model.slot

	fun launch(block: suspend CoroutineScope.() -> Unit): Job = model.launch(block = block)
	inline fun <reified A : Any> navigate(route: A, options: NavOptions? = null, extras: Navigator.Extras? = null) = model.navigate(route, options, extras)
	inline fun <reified T : Screen<Unit>> navigate(options: NavOptions? = null, extras: Navigator.Extras? = null) = model.navigate<T>(options, extras)
	fun deeplink(uri: Uri) = model.deeplink.process(uri)

	fun <T> monitor(state: () -> T, action: suspend (T) -> Unit) = launch { snapshotFlow(state).collectLatest(action) }

	open suspend fun initialize() {}
	open suspend fun update() {}

	@Composable
	abstract fun Content()

	open val fabIcon: ImageVector? get() = null
	open val fabCanExpand: Boolean get() = false
	open val fabMenus: Array<FABAction> = emptyArray()
	open suspend fun onFabClick() {}

	@Composable
	open fun Floating() {}
}

@Stable
class AppModel(
	val navController: NavController
) : ViewModel() {
	val deeplink = DeepLink(this)

	val msgPart = ScreenPartMsg(this)
	val worldPart = ScreenPartWorld(this)
	val musicPart = ScreenPartMusic(this)
	val discoveryPart = ScreenPartDiscovery(this)
	val mePart = ScreenPartMe(this)
	val parts = arrayOf(msgPart, worldPart, musicPart, discoveryPart, mePart)

	val slot = SubScreenSlot(viewModelScope)

	fun launch(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch(block = block)
	inline fun <reified A : Any> navigate(route: A, options: NavOptions? = null, extras: Navigator.Extras? = null) = navController.navigate(route, options, extras)
	inline fun <reified T : Screen<Unit>> navigate(options: NavOptions? = null, extras: Navigator.Extras? = null) = navController.navigate(route<T>(), options, extras)
	fun pop() {
		if (navController.previousBackStackEntry != null) navController.popBackStack()
	}
}

//@Composable
//fun AppUI(
//	modifier: Modifier = Modifier.fillMaxSize()
//) {
//	val navController = rememberNavController()
//	val appModel: AppModel = viewModel { AppModel(navController) }
//	val animationSpeed = LocalAnimationSpeed.current
//
//	DisposableEffect(Unit) {
//		DeepLinkHandler.listener = { uri ->
//			appModel.deeplink.process(uri)
//		}
//		onDispose {
//			DeepLinkHandler.listener = null
//		}
//	}
//
//	NavHost(
//		modifier = modifier.background(MaterialTheme.colorScheme.background),
//		navController = navController,
//		startDestination = route<ScreenMain>(),
//		enterTransition = {
//			slideIntoContainer(
//				towards = AnimatedContentTransitionScope.SlideDirection.Start,
//				animationSpec = tween(
//					durationMillis = animationSpeed,
//					easing = FastOutSlowInEasing
//				)
//			)
//		},
//		exitTransition = {
//			slideOutOfContainer(
//				towards = AnimatedContentTransitionScope.SlideDirection.End,
//				animationSpec = tween(
//					durationMillis = animationSpeed,
//					easing = FastOutSlowInEasing
//				)
//			)
//		}
//	) {
//		with(ScreenRouteScope(this, appModel)) {
//			screens()
//		}
//	}
//}

@Composable
fun AppEntry(
	fill: Boolean = true,
	modifier: Modifier = Modifier.fillMaxSize(),
	content: @Composable BoxWithConstraintsScope.() -> Unit
) {
	App(
		deviceFactory = { maxWidth, maxHeight -> if (fill) Device(maxWidth, maxHeight) else Device(maxWidth) },
		themeMode = app.config.themeMode,
		fontScale = app.config.fontScale,
		mainFontResource = Res.font.xwwk,
		modifier = modifier,
		localProvider = arrayOf(
			LocalAnimationSpeed provides app.config.animationSpeed,
			localBalloonTipEnabled provides app.config.enabledTip
		),
		content = content
	)
}

@Composable
fun ScreenEntry(modifier: Modifier = Modifier.fillMaxSize()) {
	AppScreen<ScreenMain>(modifier = modifier) {
		screen(::ScreenMain)
	}
}