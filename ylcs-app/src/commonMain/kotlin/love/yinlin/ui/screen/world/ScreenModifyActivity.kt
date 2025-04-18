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
import love.yinlin.extension.findAssign
import love.yinlin.extension.safeToSources
import love.yinlin.platform.*
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.Screen
import kotlin.collections.plus

@Stable
class ScreenModifyActivity(model: AppModel, args: Args) : Screen<ScreenModifyActivity.Args>(model) {
	@Stable
	@Serializable
	data class Args(val aid: Int) : Screen.Args

	private val aid = args.aid
	private val activities = worldPart.activities
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
			is Data.Error -> slot.tip.error(result.message)
		}
	}

	private suspend fun modifyPicture(path: Path) {
		slot.loading.open()
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
				input.pic = Picture(it.picPath(newPic))
				it.copy(pic = newPic)
			}
			is Data.Error -> slot.tip.error(result.message)
		}
		slot.loading.hide()
	}

	private suspend fun deletePicture() {
		slot.loading.open()
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
			is Data.Error -> slot.tip.error(result.message)
		}
		slot.loading.hide()
	}

	private suspend fun addPictures(files: List<Path>) {
		slot.loading.open()
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
				input.pics += newPics.map { pic -> Picture(it.picPath(pic)) }
				it.copy(pics = it.pics + newPics)
			}
			is Data.Error -> slot.tip.error(result.message)
		}
		slot.loading.hide()
	}

	private suspend fun modifyPictures(index: Int) {
		PicturePicker.pick()?.use { source ->
			OS.Storage.createTempFile { sink ->
				ImageProcessor(ImageCompress, quality = ImageQuality.High).process(source, sink)
			}
		}?.let { path ->
			slot.loading.open()
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
				is Data.Error -> slot.tip.error(result.message)
			}
			slot.loading.hide()
		}
	}

	private suspend fun deletePictures(index: Int) {
		slot.loading.open()
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
			is Data.Error -> slot.tip.error(result.message)
		}
		slot.loading.hide()
	}

	@Composable
	override fun content() {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = "修改活动",
			onBack = { pop() },
			actions = {
				ActionSuspend(
					icon = Icons.Outlined.Check,
					enabled = input.canSubmit
				) {
					modifyActivity()
				}
			},
			slot = slot
		) {
			ActivityInfoLayout(
				input = input,
				onPicAdd = { launch { modifyPicture(it) } },
				onPicDelete = { launch { deletePicture() } },
				onPicsAdd = { launch { addPictures(it) } },
				onPicsDelete = { launch { deletePictures(it) } },
				onPicsClick = { _, index -> launch { modifyPictures(index) } }
			)
		}
	}
}