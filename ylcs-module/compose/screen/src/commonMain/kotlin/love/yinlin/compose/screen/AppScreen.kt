package love.yinlin.compose.screen

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import love.yinlin.compose.LocalAnimationSpeed

@Composable
inline fun <reified Main : BasicScreen<Unit>> AppScreen(
	modifier: Modifier = Modifier,
	deeplink: DeepLink,
	crossinline screens: ScreenBuilder.() -> Unit
) {
	val navController = rememberNavController()
	val screenManager = remember(navController) { ScreenManager(navController) }
	val animationSpeed = LocalAnimationSpeed.current

	DeepLink.Register(deeplink, screenManager)

	NavHost(
		navController = navController,
		startDestination = route<Main>(),
		modifier = modifier.background(MaterialTheme.colorScheme.background),
		enterTransition = {
			slideIntoContainer(
				towards = AnimatedContentTransitionScope.SlideDirection.Start,
				animationSpec = tween(
					durationMillis = animationSpeed,
					easing = FastOutSlowInEasing
				)
			)
		},
		exitTransition = {
			slideOutOfContainer(
				towards = AnimatedContentTransitionScope.SlideDirection.End,
				animationSpec = tween(
					durationMillis = animationSpeed,
					easing = FastOutSlowInEasing
				)
			)
		}
	) {
		ScreenBuilder(this, screenManager).screens()
	}
}