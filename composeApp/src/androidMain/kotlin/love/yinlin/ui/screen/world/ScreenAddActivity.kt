package love.yinlin.ui.screen.world

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
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
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.screen.common.ScreenImagePreview

@Stable
@Serializable
actual data object ScreenAddActivity : Screen<ScreenAddActivity.Model> {
	actual class Model(model: AppModel) : Screen.Model(model) {
		val input = ActivityInputState()

		fun updatePic(lifecycle: LifecycleOwner, image: ImageBitmap) {
			launch {
				loading.isOpen = true
				val file = compressImage(
					lifecycle = lifecycle,
					image = image,
					maxFileSize = 1024,
					quality = 100
				)
				input.pic = Picture(file.absolutePath)
				loading.isOpen = false
			}
		}

		fun addPic(lifecycle: LifecycleOwner, items: List<Uri>) {
			if (items.isNotEmpty()) launch {
				loading.isOpen = true
				val files = compressImages(
					lifecycle = lifecycle,
					images = items
				)
				for (file in files) input.pics += Picture(file.absolutePath)
				loading.isOpen = false
			}
		}

		fun addActivity() {
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

			launch {
				val result = ClientAPI.request(
					route = API.User.Activity.AddActivity,
					data = API.User.Activity.AddActivity.Request(
						token = app.config.userToken,
						activity = activity
					),
					files = { API.User.Activity.AddActivity.Files(
						pic = optionFile(input.pic?.let { Path(it.image) }) ,
						pics = file(input.pics.map { Path(it.image) })
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
					is Data.Error -> tip.error(result.message)
				}
			}
		}
	}

	actual override fun model(model: AppModel): Model = Model(model)

	@Composable
	actual override fun content(model: Model) {
		val lifecycle = LocalLifecycleOwner.current

		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = "添加活动",
			onBack = { model.pop() },
			actions = {
				ClickIcon(
					imageVector = Icons.Outlined.Check,
					enabled = model.input.canSubmit,
					modifier = Modifier.padding(end = 10.dp),
					onClick = { model.addActivity() }
				)
			},
			tip = model.tip,
			loading = model.loading
		) {
			ActivityInfoLayout(
				input = model.input,
				onPicCrop = { model.updatePic(lifecycle, it) },
				onPicDelete = { model.input.pic = null },
				onPicsAdd = { model.addPic(lifecycle, it) },
				onPicsDelete = { model.input.pics.removeAt(it) },
				onPicsClick = { items, current -> model.navigate(ScreenImagePreview(items, current)) }
			)
		}
	}
}