package love.yinlin.ui.screen.world

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.Activity
import love.yinlin.extension.LaunchFlag
import love.yinlin.extension.LaunchOnce
import love.yinlin.extension.replaceAll
import love.yinlin.platform.app
import love.yinlin.ui.component.extra.Calendar
import love.yinlin.ui.screen.MainModel

class WorldModel(val mainModel: MainModel) {
	val launchFlag = LaunchFlag()
	val activities = mutableStateListOf<Activity>()

	val pics: List<Picture> by derivedStateOf {
		val banners = mutableListOf<Picture>()
		for (activity in activities) {
			activity.picPath?.let { banners += Picture(it) }
		}
		banners
	}

	fun requestActivity() {
		mainModel.launch {
			val result = ClientAPI.request(
				route = API.User.Activity.GetActivities
			)
			if (result is Data.Success) {
				activities.replaceAll(result.data)
			}
		}
	}
}

@Composable
private fun Portrait(
	model: WorldModel
) {
//	Banner(
//		pics = model.pics,
//		aspectRatio = 2f,
//		spacing = 40.dp,
//		interval = 3000L,
//		modifier = Modifier.fillMaxWidth()
//	)

	Calendar(
		modifier = Modifier.fillMaxWidth().aspectRatio(1f)
	)
}

@Composable
private fun Landscape(
	model: WorldModel
) {

}


@Composable
fun ScreenWorld(model: MainModel) {
	if (app.isPortrait) Portrait(model = model.worldModel)
	else Landscape(model = model.worldModel)

	LaunchOnce(model.worldModel.launchFlag) {
		model.worldModel.requestActivity()
	}
}