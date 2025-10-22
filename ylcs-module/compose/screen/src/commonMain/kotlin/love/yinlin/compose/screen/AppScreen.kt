package love.yinlin.compose.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val screenManager = remember(navController) { ScreenManager(navController) }

    NavHost(
        navController = navController,
        startDestination = "",
        modifier = modifier,
    ) {

    }
}