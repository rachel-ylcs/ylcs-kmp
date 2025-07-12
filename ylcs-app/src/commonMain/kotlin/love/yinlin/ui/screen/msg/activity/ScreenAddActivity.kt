package love.yinlin.ui.screen.msg.activity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.safeToSources
import love.yinlin.platform.app
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.screen.dialog.FloatingDialogCrop
import love.yinlin.ui.screen.common.ScreenImagePreview

@Stable
class ScreenAddActivity(model: AppModel) : CommonSubScreen(model) {
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
				pic = file(input.pic?.let { SystemFileSystem.source(Path(it)) }) ,
				pics = file(input.pics.safeToSources { SystemFileSystem.source(Path(it.image)) })
			) }
		)
		when (result) {
			is Success -> {
				val (aid, serverPic, serverPics) = result.data
				msgPart.activities.add(0, activity.copy(
					aid = aid,
					pic = serverPic,
					pics = serverPics
				))
				pop()
			}
			is Failure -> slot.tip.error(result.message)
		}
	}

	override val title: String = "添加活动"

	@Composable
	override fun ActionScope.RightActions() {
		ActionSuspend(
			icon = Icons.Outlined.Check,
            tip = "提交",
			enabled = input.canSubmit
		) {
			addActivity()
		}
	}

	@Composable
	override fun SubContent(device: Device) {
        ActivityInfoLayout(
            cropDialog = cropDialog,
            input = input,
            onPicAdd = { input.pic = it.toString() },
            onPicDelete = { input.pic = null },
            onPicsAdd = { for (file in it) input.pics += Picture(file.toString()) },
            onPicsDelete = { input.pics.removeAt(it) },
            onPicsClick = { items, current -> navigate(ScreenImagePreview.Args(items, current)) }
        )
	}

	private val cropDialog = FloatingDialogCrop()

	@Composable
	override fun Floating() {
		cropDialog.Land()
	}
}