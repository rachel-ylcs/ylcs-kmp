package love.yinlin.screen.msg.activity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastMap
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.app
import love.yinlin.compose.Device
import love.yinlin.data.compose.ImageQuality
import love.yinlin.compose.graphics.ImageCompress
import love.yinlin.compose.graphics.ImageProcessor
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.Data
import love.yinlin.data.compose.Picture
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.findAssign
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.screen.common.ScreenMain
import love.yinlin.screen.msg.SubScreenMsg
import love.yinlin.compose.ui.floating.FloatingDialogCrop
import love.yinlin.io.safeToSources

@Stable
class ScreenModifyActivity(manager: ScreenManager, private val aid: Int) : Screen(manager) {
	private val activities = manager.get<ScreenMain>().get<SubScreenMsg>().activities
	private val input = ActivityInputState(activities.find { it.aid == aid })

	private suspend fun modifyActivity() {
		val ts = input.ts
		val title = input.titleString
		val content = input.contentString
		val showstart = input.showstartString
		val damai = input.damaiString
		val maoyan = input.maoyanString
		val link = input.linkString
		val result = ClientAPI.request(
			route = API.User.Activity.ModifyActivityInfo,
			data = API.User.Activity.ModifyActivityInfo.Request(
				token = app.config.userToken,
				activity = Activity(
					aid = aid,
					ts = ts,
					title = title,
					content = content,
					pic = null,
					pics = emptyList(),
					showstart = showstart,
					damai = damai,
					maoyan = maoyan,
					link = link
				)
			)
		)
		when (result) {
			is Data.Success -> {
				activities.findAssign(predicate = { it.aid == aid }) {
					it.copy(
						ts = ts,
						title = title,
						content = content,
						showstart = showstart,
						damai = damai,
						maoyan = maoyan,
						link = link
					)
				}
				slot.tip.success(result.message)
			}
			is Data.Failure -> slot.tip.error(result.message)
		}
	}

	private suspend fun modifyPicture(path: Path) {
		slot.loading.openSuspend()
		val result = ClientAPI.request(
			route = API.User.Activity.ModifyActivityPicture,
			data = API.User.Activity.ModifyActivityPicture.Request(
				token = app.config.userToken,
				aid = aid
			),
			files = { API.User.Activity.ModifyActivityPicture.Files(
				pic = file(SystemFileSystem.source(path))
			) }
		)
		when (result) {
			is Data.Success -> activities.findAssign(predicate = { it.aid == aid }) {
				val newPic = result.data
				input.pic = it.picPath(newPic)
				it.copy(pic = newPic)
			}
			is Data.Failure -> slot.tip.error(result.message)
		}
		slot.loading.close()
	}

	private suspend fun deletePicture() {
		slot.loading.openSuspend()
		val result = ClientAPI.request(
			route = API.User.Activity.DeleteActivityPicture,
			data = API.User.Activity.DeleteActivityPicture.Request(
				token = app.config.userToken,
				aid = aid
			)
		)
		when (result) {
			is Data.Success -> activities.findAssign(predicate = { it.aid == aid }) {
				input.pic = null
				it.copy(pic = null)
			}
			is Data.Failure -> slot.tip.error(result.message)
		}
		slot.loading.close()
	}

	private suspend fun addPictures(files: List<Path>) {
		slot.loading.openSuspend()
		val result = ClientAPI.request(
			route = API.User.Activity.AddActivityPictures,
			data = API.User.Activity.AddActivityPictures.Request(
				token = app.config.userToken,
				aid = aid
			),
			files = { API.User.Activity.AddActivityPictures.Files(
				pics = file(files.safeToSources { SystemFileSystem.source(it) })
			) }
		)
		when (result) {
			is Data.Success -> activities.findAssign(predicate = { it.aid == aid }) {
				val newPics = result.data
				input.pics += newPics.fastMap { pic -> Picture(it.picPath(pic)) }
				it.copy(pics = it.pics + newPics)
			}
			is Data.Failure -> slot.tip.error(result.message)
		}
		slot.loading.close()
	}

	private suspend fun modifyPictures(index: Int) {
		app.picker.pickPicture()?.use { source ->
			app.os.storage.createTempFile { sink ->
				ImageProcessor(ImageCompress, quality = ImageQuality.High).process(source, sink)
			}
		}?.let { path ->
			slot.loading.openSuspend()
			val result = ClientAPI.request(
				route = API.User.Activity.ModifyActivityPictures,
				data = API.User.Activity.ModifyActivityPictures.Request(
					token = app.config.userToken,
					aid = aid,
					index = index
				),
				files = { API.User.Activity.ModifyActivityPictures.Files(
					pic = file(SystemFileSystem.source(path))
				) }
			)
			when (result) {
				is Data.Success -> activities.findAssign(predicate = { it.aid == aid }) {
					val newPic = result.data
					input.pics[index] = Picture(it.picPath(newPic))
					it.copy(pics = it.pics.toMutableList().also { pics -> pics[index] = newPic })
				}
				is Data.Failure -> slot.tip.error(result.message)
			}
			slot.loading.close()
		}
	}

	private suspend fun deletePictures(index: Int) {
		slot.loading.openSuspend()
		val result = ClientAPI.request(
			route = API.User.Activity.DeleteActivityPictures,
			data = API.User.Activity.DeleteActivityPictures.Request(
				token = app.config.userToken,
				aid = aid,
				index = index
			)
		)
		when (result) {
			is Data.Success -> activities.findAssign(predicate = { it.aid == aid }) {
				input.pics.removeAt(index)
				it.copy(pics = it.pics.toMutableList().also { pics -> pics.removeAt(index) })
			}
			is Data.Failure -> slot.tip.error(result.message)
		}
		slot.loading.close()
	}

	override val title: String = "修改活动"

	@Composable
	override fun ActionScope.RightActions() {
		ActionSuspend(
			icon = Icons.Outlined.Check,
            tip = "提交",
			enabled = input.canSubmit
		) {
			modifyActivity()
		}
	}

	@Composable
	override fun Content(device: Device) {
        ActivityInfoLayout(
            cropDialog = cropDialog,
            input = input,
            onPicAdd = { launch { modifyPicture(it) } },
            onPicDelete = { launch { deletePicture() } },
            onPicsAdd = { launch { addPictures(it) } },
            onPicsDelete = { launch { deletePictures(it) } },
            onPicsClick = { _, index -> launch { modifyPictures(index) } }
        )
	}

	private val cropDialog = this land FloatingDialogCrop()
}