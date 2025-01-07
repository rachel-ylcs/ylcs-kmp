package love.yinlin

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import love.yinlin.model.AppModel
import love.yinlin.screen.Route
import love.yinlin.screen.ScreenMain

@Composable
fun App(modifier: Modifier = Modifier.fillMaxSize()) {
	val navController = rememberNavController()
	val appModel = viewModel { AppModel(navController) }
	NavHost(
		modifier = modifier.background(MaterialTheme.colorScheme.background),
		navController = navController,
		startDestination = Route.Main,
	) {
		composable<Route.Main> {
			ScreenMain(appModel)
		}
	}
}

@Composable
fun AppWrapper(
	darkMode: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	CompositionLocalProvider(
		LocalDensity provides Density(
			density = app.screenWidth / app.designWidth.value,
			fontScale = app.fontScale
		)
	) {
		RachelTheme(darkMode) {
			Box(modifier = Modifier.fillMaxSize()) {
				content()
			}
		}
	}
}