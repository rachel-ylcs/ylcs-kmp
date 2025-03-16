package love.yinlin.ui.screen.world

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.ScreenModel
import love.yinlin.common.compressImage
import love.yinlin.common.compressImages
import love.yinlin.common.screen
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.Activity
import love.yinlin.extension.DateEx
import love.yinlin.platform.app
import love.yinlin.ui.Route
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.DialogCrop
import love.yinlin.ui.component.image.DialogCropState
import love.yinlin.ui.component.image.ImageAdder
import love.yinlin.ui.component.image.PictureSelector
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.DockedDatePicker
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.screen.DialogLoading
import love.yinlin.ui.component.screen.DialogState
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.screen.Tip
import love.yinlin.ui.component.screen.TipState
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState

private class AddActivityModel(
	private val model: AppModel
) : ScreenModel() {
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
				model.mainModel.worldModel.activities += Activity(
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
				)
				model.pop()
			}
			else if (result is Data.Error) tip.error(result.message)
		}
	}
}

@Composable
actual fun ScreenAddActivity(model: AppModel) {
	val screenModel = screen { AddActivityModel(model) }

	val lifecycle = LocalLifecycleOwner.current
	val picPicker = PictureSelector {
		screenModel.cropState.uri = it
	}
	val picsPicker = PictureSelector((9 - screenModel.pics.size).coerceAtLeast(1)) {
		screenModel.addPic(lifecycle, it)
	}

	SubScreen(
		modifier = Modifier.fillMaxSize(),
		title = "添加活动",
		onBack = { model.pop() },
		actions = {
			ClickIcon(
				imageVector = Icons.Default.Check,
				enabled = screenModel.canSubmit,
				modifier = Modifier.padding(end = 10.dp),
				onClick = { screenModel.addActivity() }
			)
		}
	) {
		Column(
			modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(10.dp),
		) {
			TextInput(
				state = screenModel.title,
				hint = "活动名称(可空, 2-4字)",
				maxLength = 4,
				modifier = Modifier.fillMaxWidth()
			)
			DockedDatePicker(
				hint = "活动时间(可空)",
				onDateSelected = { screenModel.date = it },
				modifier = Modifier.fillMaxWidth()
			)
			TextInput(
				state = screenModel.content,
				hint = "活动内容",
				maxLength = 512,
				maxLines = 10,
				modifier = Modifier.fillMaxWidth()
			)
			TextInput(
				state = screenModel.showstart,
				hint = "秀动ID(可空)",
				maxLength = 1024,
				maxLines = 5,
				modifier = Modifier.fillMaxWidth()
			)
			TextInput(
				state = screenModel.damai,
				hint = "大麦ID(可空)",
				maxLength = 16,
				modifier = Modifier.fillMaxWidth()
			)
			TextInput(
				state = screenModel.maoyan,
				hint = "猫眼ID(可空)",
				maxLength = 16,
				modifier = Modifier.fillMaxWidth()
			)
			TextInput(
				state = screenModel.link,
				hint = "活动链接(可空)",
				maxLength = 256,
				modifier = Modifier.fillMaxWidth()
			)
			Text(
				text = "轮播图(可空)",
				style = MaterialTheme.typography.titleMedium
			)
			val pic = screenModel.pic
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
				pics = screenModel.pics,
				size = 80.dp,
				modifier = Modifier.fillMaxWidth(),
				onAdd = { picsPicker.select() },
				onDelete = { screenModel.deletePic(it) },
				onClick = { model.navigate(Route.ImagePreview(screenModel.pics, it)) }
			)
		}
	}

	if (screenModel.cropState.isOpen) {
		DialogCrop(
			state = screenModel.cropState,
			aspectRatio = 2f,
			onCropped = { screenModel.updatePic(lifecycle, it) }
		)
	}

	Tip(state = screenModel.tip)

	if (screenModel.loadingState.isOpen) {
		DialogLoading(state = screenModel.loadingState)
	}
}