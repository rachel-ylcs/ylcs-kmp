package love.yinlin.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import love.yinlin.model.AppModel

@Composable
fun ScreenMsg(
	model: AppModel,
	navController: NavController
) {
	Text("ScreenMsg")
}