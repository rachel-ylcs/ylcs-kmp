package love.yinlin.ui.screen.world

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.safeToSources
import love.yinlin.platform.app
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.screen.common.ScreenImagePreview

@Stable
@Serializable
data object ScreenAddActivity : Screen<ScreenAddActivity.Model> {
	class Model(model: AppModel) : Screen.Model(model) {
		val input = ActivityInputState()

		suspend fun addActivity() {
			val activity = Activity(
				aid = 0,
				ts = input.ts,
				title = input.titleString,
				content = input.contentString,
				pic = null,
				pics = emptyList(),
				showstart = input.showstartString,
				damai = input.damaiString,
				maoyan = input.maoyanString,
				link = input.linkString
			)

			val result = ClientAPI.request(
				route = API.User.Activity.AddActivity,
				data = API.User.Activity.AddActivity.Request(
					token = app.config.userToken,
					activity = activity
				),
				files = { API.User.Activity.AddActivity.Files(
					pic = file(input.pic?.let { SystemFileSystem.source(Path(it.image)) }) ,
					pics = file(input.pics.safeToSources { SystemFileSystem.source(Path(it.image)) })
				) }
			)
			when (result) {
				is Data.Success -> {
					val (aid, serverPic, serverPics) = result.data
					part<ScreenPartWorld>().activities.add(0, activity.copy(
						aid = aid,
						pic = serverPic,
						pics = serverPics
					))
					pop()
				}
				is Data.Error -> slot.tip.error(result.message)
			}
		}
	}

	override fun model(model: AppModel): Model = Model(model)

	@Composable
	override fun content(model: Model) {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = "添加活动",
			onBack = { model.pop() },
			actions = {
				ActionSuspend(icon = Icons.Outlined.Check, enabled = model.input.canSubmit) {
					model.addActivity()
				}
			},
			slot = model.slot
		) {
			ActivityInfoLayout(
				input = model.input,
				onPicAdd = { model.input.pic = Picture(it.toString()) },
				onPicDelete = { model.input.pic = null },
				onPicsAdd = { for (file in it) model.input.pics += Picture(file.toString()) },
				onPicsDelete = { model.input.pics.removeAt(it) },
				onPicsClick = { items, current -> model.navigate(ScreenImagePreview(items, current)) }
			)
		}
	}
}