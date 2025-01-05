package love.yinlin.model

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController

class AppModel(
	val navController: NavController
) : ViewModel() {
	val msgModel = MsgModel()
}