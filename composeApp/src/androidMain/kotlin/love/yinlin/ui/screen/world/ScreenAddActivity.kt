package love.yinlin.ui.screen.world

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.datetime.LocalDate
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
import love.yinlin.extension.DateEx
import love.yinlin.platform.app
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.component.image.*
import love.yinlin.ui.component.input.DockedDatePicker
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.screen.*
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.common.ScreenImagePreview

@Stable
@Serializable
actual data object ScreenAddActivity : Screen<ScreenAddActivity.Model> {
	actual class Model(model: AppModel) : Screen.Model(model) {
		val title = TextInputState()
		var date: LocalDate? by mutableStateOf(null)
		val content = TextInputState()
		val showstart = TextInputState()
		val damai = TextInputState()
		val maoyan = TextInputState()
		val link = TextInputState()
		var pic: Picture? by mutableStateOf(null)
		val pics = mutableStateListOf<Picture>()

		// [活动要求]
		// 必须包含内容
		// 必须属于以下之一: <包含轮播图> <包含活动名称及活动日期>
		val canSubmit by derivedStateOf {
			!title.overflow && content.ok
					&& !showstart.overflow && !damai.overflow && !maoyan.overflow && !link.overflow
					&& (pic != null || (title.text.isNotEmpty() && date != null))
		}

		val cropState = DialogCropState()
		val tip = TipState()
		val loadingState = DialogState()

		fun updatePic(lifecycle: LifecycleOwner, image: ImageBitmap) {
			launch {
				loadingState.isOpen = true
				val file = compressImage(
					lifecycle = lifecycle,
					image = image,
					maxFileSize = 1024,
					quality = 100
				)
				pic = Picture(file.absolutePath)
				loadingState.isOpen = false
			}
		}

		fun addPic(lifecycle: LifecycleOwner, items: List<Uri>) {
			if (items.isNotEmpty()) launch {
				loadingState.isOpen = true
				val files = compressImages(
					lifecycle = lifecycle,
					images = items
				)
				for (file in files) pics += Picture(file.absolutePath)
				loadingState.isOpen = false
			}
		}

		fun deletePic(index: Int) {
			pics.removeAt(index)
		}

		fun addActivity() {
			val actualTs = date?.let { DateEx.Formatter.standardDate.format(it) }
			val actualTitle = title.text.ifEmpty { null }
			val actualContent = content.text
			val actualShowstart = showstart.text.ifEmpty { null }
			val actualDamai = damai.text.ifEmpty { null }
			val actualMaoyan = maoyan.text.ifEmpty { null }
			val actualLink = link.text.ifEmpty { null }
			val activity = Activity(
				aid = 0,
				ts = actualTs,
				title = actualTitle,
				content = actualContent,
				pic = null,
				pics = emptyList(),
				showstart = actualShowstart,
				damai = actualDamai,
				maoyan = actualMaoyan,
				link = actualLink
			)
			launch {
				val result = ClientAPI.request(
					route = API.User.Activity.AddActivity,
					data = API.User.Activity.AddActivity.Request(
						token = app.config.userToken,
						activity = activity
					),
					files = { API.User.Activity.AddActivity.Files(
						pic = file(pic?.let { Path(it.image) }) ,
						pics = file(pics.map { Path(it.image) })
					) }
				)
				if (result is Data.Success) {
					val (aid, serverPic, serverPics) = result.data
					part<ScreenPartWorld>().activities.add(0, Activity(
						aid = aid,
						ts = actualTs,
						title = actualTitle,
						content = actualContent,
						pic = serverPic,
						pics = serverPics,
						showstart = actualShowstart,
						damai = actualDamai,
						maoyan = actualMaoyan,
						link = actualLink
					))
					pop()
				}
				else if (result is Data.Error) tip.error(result.message)
			}
		}
	}

	actual override fun model(model: AppModel): Model = Model(model)

	@Composable
	actual override fun content(model: Model) {
		val lifecycle = LocalLifecycleOwner.current
		val picPicker = PictureSelector {
			model.cropState.uri = it
		}
		val picsPicker = PictureSelector((9 - model.pics.size).coerceAtLeast(1)) {
			model.addPic(lifecycle, it)
		}

		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = "添加活动",
			onBack = { model.pop() },
			actions = {
				ClickIcon(
					imageVector = Icons.Outlined.Check,
					enabled = model.canSubmit,
					modifier = Modifier.padding(end = 10.dp),
					onClick = { model.addActivity() }
				)
			}
		) {
			Column(
				modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState()),
				verticalArrangement = Arrangement.spacedBy(10.dp),
			) {
				TextInput(
					state = model.title,
					hint = "活动名称(可空, 2-4字)",
					maxLength = 4,
					modifier = Modifier.fillMaxWidth()
				)
				DockedDatePicker(
					hint = "活动时间(可空)",
					onDateSelected = { model.date = it },
					modifier = Modifier.fillMaxWidth()
				)
				TextInput(
					state = model.content,
					hint = "活动内容",
					maxLength = 512,
					maxLines = 10,
					modifier = Modifier.fillMaxWidth()
				)
				TextInput(
					state = model.showstart,
					hint = "秀动ID(可空)",
					maxLength = 1024,
					maxLines = 5,
					modifier = Modifier.fillMaxWidth()
				)
				TextInput(
					state = model.damai,
					hint = "大麦ID(可空)",
					maxLength = 16,
					modifier = Modifier.fillMaxWidth()
				)
				TextInput(
					state = model.maoyan,
					hint = "猫眼ID(可空)",
					maxLength = 16,
					modifier = Modifier.fillMaxWidth()
				)
				TextInput(
					state = model.link,
					hint = "活动链接(可空)",
					maxLength = 256,
					modifier = Modifier.fillMaxWidth()
				)
				Text(
					text = "轮播图(可空)",
					style = MaterialTheme.typography.titleMedium
				)
				val pic = model.pic
				if (pic == null) {
					Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f).clickable(onClick = {
						picPicker.select()
					})) {
						EmptyBox()
					}
				}
				else {
					WebImage(
						uri = pic.image,
						modifier = Modifier.fillMaxWidth().aspectRatio(2f),
						contentScale = ContentScale.Crop,
						onClick = { picPicker.select() }
					)
				}
				Text(
					text = "活动海报",
					style = MaterialTheme.typography.titleMedium
				)

				ImageAdder(
					maxNum = 9,
					pics = model.pics,
					size = 80.dp,
					modifier = Modifier.fillMaxWidth(),
					onAdd = { picsPicker.select() },
					onDelete = { model.deletePic(it) },
					onClick = { model.navigate(ScreenImagePreview(model.pics, it)) }
				)
			}
		}

		if (model.cropState.isOpen) {
			DialogCrop(
				state = model.cropState,
				aspectRatio = 2f,
				onCropped = { model.updatePic(lifecycle, it) }
			)
		}

		Tip(state = model.tip)

		if (model.loadingState.isOpen) {
			DialogLoading(state = model.loadingState)
		}
	}
}