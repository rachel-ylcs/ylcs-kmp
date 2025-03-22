package love.yinlin.ui.screen.world

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.compressImage
import love.yinlin.common.compressImages
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.Activity
import love.yinlin.platform.app
import love.yinlin.ui.component.input.RachelButton
import love.yinlin.ui.component.image.PictureSelector
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.Screen

@Stable
@Serializable
actual data class ScreenModifyActivity actual constructor(val aid: Int) : Screen<ScreenModifyActivity.Model> {
	actual inner class Model(model: AppModel) : Screen.Model(model) {
		val activities = part<ScreenPartWorld>().activities
		val input = ActivityInputState(activities.find { it.aid == aid })

		private fun processActivity(block: (Activity) -> Activity) {
			val index = activities.indexOfFirst { it.aid == aid }
			if (index != -1) activities[index] = block(activities[index])
		}

		fun modifyActivity() {
			launch {
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
						processActivity {
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
						tip.success(result.message)
					}
					is Data.Error -> tip.error(result.message)
				}
			}
		}

		fun modifyPicture(lifecycle: LifecycleOwner, image: ImageBitmap) {
			launch {
				loading.isOpen = true
				val file = compressImage(
					lifecycle = lifecycle,
					image = image,
					maxFileSize = 1024,
					quality = 100
				)
				val result = ClientAPI.request(
					route = API.User.Activity.ModifyActivityPicture,
					data = API.User.Activity.ModifyActivityPicture.Request(
						token = app.config.userToken,
						aid = aid
					),
					files = { API.User.Activity.ModifyActivityPicture.Files(
						pic = file(Path(file.absolutePath))
					) }
				)
				when (result) {
					is Data.Success -> {
						processActivity {
							val newPic = result.data
							input.pic = Picture(it.picPath(newPic))
							it.copy(pic = newPic)
						}
					}
					is Data.Error -> tip.error(result.message)
				}
				loading.isOpen = false
			}
		}

		fun deletePicture() {
			launch {
				loading.isOpen = true
				val result = ClientAPI.request(
					route = API.User.Activity.DeleteActivityPicture,
					data = API.User.Activity.DeleteActivityPicture.Request(
						token = app.config.userToken,
						aid = aid
					)
				)
				when (result) {
					is Data.Success -> {
						processActivity {
							input.pic = null
							it.copy(pic = null)
						}
					}
					is Data.Error -> tip.error(result.message)
				}
				loading.isOpen = false
			}
		}

		fun addPictures(lifecycle: LifecycleOwner, items: List<Uri>) {
			launch {
				loading.isOpen = true
				val files = compressImages(
					lifecycle = lifecycle,
					images = items,
					maxFileSize = 1024,
					quality = 100
				)
				val result = ClientAPI.request(
					route = API.User.Activity.AddActivityPictures,
					data = API.User.Activity.AddActivityPictures.Request(
						token = app.config.userToken,
						aid = aid
					),
					files = { API.User.Activity.AddActivityPictures.Files(
						pics = file(files.map { Path(it.absolutePath) })
					) }
				)
				when (result) {
					is Data.Success -> {
						processActivity { oldActivity ->
							val newPics = result.data
							input.pics += newPics.map { Picture(oldActivity.picPath(it)) }
							oldActivity.copy(pics = oldActivity.pics + newPics)
						}
					}
					is Data.Error -> tip.error(result.message)
				}
				loading.isOpen = false
			}
		}

		fun modifyPictures(lifecycle: LifecycleOwner, index: Int, uri: Uri) {
			launch {
				loading.isOpen = true
				val file = compressImage(
					lifecycle = lifecycle,
					image = uri,
					maxFileSize = 1024,
					quality = 100
				)
				val result = ClientAPI.request(
					route = API.User.Activity.ModifyActivityPictures,
					data = API.User.Activity.ModifyActivityPictures.Request(
						token = app.config.userToken,
						aid = aid,
						index = index
					),
					files = { API.User.Activity.ModifyActivityPictures.Files(
						pic = file(Path(file.absolutePath))
					) }
				)
				when (result) {
					is Data.Success -> {
						processActivity { oldActivity ->
							val newPic = result.data
							input.pics[index] = Picture(oldActivity.picPath(newPic))
							oldActivity.copy(pics = oldActivity.pics.toMutableList().also { it[index] = newPic })
						}
					}
					is Data.Error -> tip.error(result.message)
				}
			}
		}

		fun deletePictures(index: Int) {
			launch {
				loading.isOpen = true
				val result = ClientAPI.request(
					route = API.User.Activity.DeleteActivityPictures,
					data = API.User.Activity.DeleteActivityPictures.Request(
						token = app.config.userToken,
						aid = aid,
						index = index
					)
				)
				when (result) {
					is Data.Success -> {
						processActivity { oldActivity ->
							input.pics.removeAt(index)
							oldActivity.copy(pics = oldActivity.pics.toMutableList().also { it.removeAt(index) })
						}
					}
					is Data.Error -> tip.error(result.message)
				}
				loading.isOpen = false
			}
		}
	}

	actual override fun model(model: AppModel): Model = Model(model)

	@Composable
	actual override fun content(model: Model) {
		val lifecycle = LocalLifecycleOwner.current

		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = "修改活动",
			onBack = { model.pop() },
			tip = model.tip,
			loading = model.loading
		) {
			var selectIndex = remember { -1 }
			val picker = PictureSelector {
				model.modifyPictures(lifecycle, selectIndex, it)
			}

			ActivityInfoLayout(
				input = model.input,
				content = {
					RachelButton(
						text = "更新内容",
						modifier = Modifier.align(Alignment.CenterHorizontally),
						onClick = { model.modifyActivity() }
					)
				},
				onPicCrop = { model.modifyPicture(lifecycle, it) },
				onPicDelete = { model.deletePicture() },
				onPicsAdd = { model.addPictures(lifecycle, it) },
				onPicsDelete = { model.deletePictures(it) },
				onPicsClick = { _, index ->
					selectIndex = index
					picker.select()
				}
			)
		}
	}
}