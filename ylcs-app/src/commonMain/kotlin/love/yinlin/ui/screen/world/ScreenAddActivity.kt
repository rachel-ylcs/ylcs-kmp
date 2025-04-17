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
class ScreenAddActivity(model: AppModel) : Screen<ScreenAddActivity.Args>(model) {
	@Stable
	@Serializable
	data object Args : Screen.Args

	private val input = ActivityInputState()

	private suspend fun addActivity() {
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

	@Composable
	override fun content() {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = "添加活动",
			onBack = { pop() },
			actions = {
				ActionSuspend(
					icon = Icons.Outlined.Check,
					enabled = input.canSubmit
				) {
					addActivity()
				}
			},
			slot = slot
		) {
			ActivityInfoLayout(
				input = input,
				onPicAdd = { input.pic = Picture(it.toString()) },
				onPicDelete = { input.pic = null },
				onPicsAdd = { for (file in it) input.pics += Picture(file.toString()) },
				onPicsDelete = { input.pics.removeAt(it) },
				onPicsClick = { items, current -> navigate(ScreenImagePreview.Args(items, current)) }
			)
		}
	}
}