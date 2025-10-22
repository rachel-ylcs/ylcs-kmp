package love.yinlin.ui.screen.msg.activity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastMap
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.compose.Device
import love.yinlin.compose.data.ImageQuality
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.findAssign
import love.yinlin.extension.safeToSources
import love.yinlin.platform.*
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.screen.dialog.FloatingDialogCrop

@Stable
class ScreenModifyActivity(model: AppModel, private val args: Args) : SubScreen<ScreenModifyActivity.Args>(model) {
	@Stable
	@Serializable
	data class Args(val aid: Int)

	private val activities = msgPart.activities
	private val input = ActivityInputState(activities.find { it.aid == args.aid })

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
					aid = args.aid,
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
				activities.findAssign(predicate = { it.aid == args.aid }) {
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
				aid = args.aid
			),
			files = { API.User.Activity.ModifyActivityPicture.Files(
				pic = file(SystemFileSystem.source(path))
			) }
		)
		when (result) {
			is Data.Success -> activities.findAssign(predicate = { it.aid == args.aid }) {
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
				aid = args.aid
			)
		)
		when (result) {
			is Data.Success -> activities.findAssign(predicate = { it.aid == args.aid }) {
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
				aid = args.aid
			),
			files = { API.User.Activity.AddActivityPictures.Files(
				pics = file(files.safeToSources { SystemFileSystem.source(it) })
			) }
		)
		when (result) {
			is Data.Success -> activities.findAssign(predicate = { it.aid == args.aid }) {
				val newPics = result.data
				input.pics += newPics.fastMap { pic -> Picture(it.picPath(pic)) }
				it.copy(pics = it.pics + newPics)
			}
			is Data.Failure -> slot.tip.error(result.message)
		}
		slot.loading.close()
	}

	private suspend fun modifyPictures(index: Int) {
		Picker.pickPicture()?.use { source ->
			OS.Storage.createTempFile { sink ->
				ImageProcessor(ImageCompress, quality = ImageQuality.High).process(source, sink)
			}
		}?.let { path ->
			slot.loading.openSuspend()
			val result = ClientAPI.request(
				route = API.User.Activity.ModifyActivityPictures,
				data = API.User.Activity.ModifyActivityPictures.Request(
					token = app.config.userToken,
					aid = args.aid,
					index = index
				),
				files = { API.User.Activity.ModifyActivityPictures.Files(
					pic = file(SystemFileSystem.source(path))
				) }
			)
			when (result) {
				is Data.Success -> activities.findAssign(predicate = { it.aid == args.aid }) {
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
				aid = args.aid,
				index = index
			)
		)
		when (result) {
			is Data.Success -> activities.findAssign(predicate = { it.aid == args.aid }) {
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
	override fun SubContent(device: Device) {
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

	private val cropDialog = FloatingDialogCrop()

	@Composable
	override fun Floating() {
		cropDialog.Land()
	}
}