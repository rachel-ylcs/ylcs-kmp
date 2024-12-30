package love.yinlin

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import love.yinlin.model.AppModel
import love.yinlin.platform.AppContext
import love.yinlin.screen.Route
import love.yinlin.screen.ScreenMain

val LocalAppContext = staticCompositionLocalOf<AppContext> {
	error("CompositionLocal AppContext not present")
}

@Composable
fun App(appModel: AppModel) {
	val navController = rememberNavController()
	NavHost(
		modifier = Modifier.fillMaxSize(),
		navController = navController,
		startDestination = Route.Main,
	) {
		composable<Route.Main> {
			ScreenMain(appModel, navController)
		}
	}
}

@Composable
fun AppWrapper(
	appContext: AppContext,
	darkMode: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	CompositionLocalProvider(
		LocalAppContext provides appContext,
		LocalDensity provides Density(
			density = appContext.screenWidth / appContext.designWidth.value,
			fontScale = appContext.fontScale
		)
	) {
		RachelTheme(content, darkMode)
	}
}