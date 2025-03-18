package love.yinlin.ui.screen.world

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.datetime.LocalDate
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.Activity
import love.yinlin.extension.DateEx
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.DialogCrop
import love.yinlin.ui.component.image.DialogCropState
import love.yinlin.ui.component.image.ImageAdder
import love.yinlin.ui.component.image.PictureSelector
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.DockedDatePicker
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState

@Stable
class ActivityInputState(initActivity: Activity? = null) {
	val initDate = initActivity?.ts?.let {
		try { DateEx.Formatter.standardDate.parse(it) }
		catch (_: Throwable) { null }
	}

	internal val title = TextInputState(initActivity?.title ?: "")
	internal var date: LocalDate? by mutableStateOf(initDate)
	internal val content = TextInputState(initActivity?.content ?: "")
	internal val showstart = TextInputState(initActivity?.showstart ?: "")
	internal val damai = TextInputState(initActivity?.damai ?: "")
	internal val maoyan = TextInputState(initActivity?.maoyan ?: "")
	internal val link = TextInputState(initActivity?.link ?: "")
	var pic: Picture? by mutableStateOf(initActivity?.picPath?.let { Picture(it) })
	val pics = initActivity?.let {
		it.pics.map { name -> Picture(it.picPath(name)) }.toMutableStateList()
	} ?: mutableStateListOf()

	internal val cropState = DialogCropState()

	// [活动要求]
	// 必须包含内容
	// 必须属于以下之一: <包含轮播图> <包含活动名称及活动日期>
	val canSubmit by derivedStateOf {
		!title.overflow && content.ok
				&& !showstart.overflow && !damai.overflow && !maoyan.overflow && !link.overflow
				&& (pic != null || (title.text.isNotEmpty() && date != null))
	}

	val ts: String? get() = date?.let { DateEx.Formatter.standardDate.format(it) }
	val titleString: String? get() = title.text.ifEmpty { null }
	val contentString: String get() = content.text
	val showstartString: String? get() = showstart.text.ifEmpty { null }
	val damaiString: String? get() = damai.text.ifEmpty { null }
	val maoyanString: String? get() = maoyan.text.ifEmpty { null }
	val linkString: String? get() = link.text.ifEmpty { null }
}

@Composable
fun ActivityInfoLayout(
	input: ActivityInputState,
	content: (@Composable ColumnScope.() -> Unit)? = null,
	onPicCrop: (ImageBitmap) -> Unit,
	onPicDelete: () -> Unit,
	onPicsAdd: (List<Uri>) -> Unit,
	onPicsDelete: (Int) -> Unit,
	onPicsClick: (List<Picture>, Int) -> Unit
) {
	val picPicker = PictureSelector { input.cropState.uri = it }
	val picsPicker = PictureSelector((9 - input.pics.size).coerceAtLeast(1)) {
		onPicsAdd(it)
	}

	Column(
		modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState()),
		verticalArrangement = Arrangement.spacedBy(10.dp),
	) {
		TextInput(
			state = input.title,
			hint = "活动名称(可空, 2-4字)",
			maxLength = 4,
			modifier = Modifier.fillMaxWidth()
		)
		DockedDatePicker(
			hint = "活动时间(可空)",
			initDate = input.initDate,
			onDateSelected = { input.date = it },
			modifier = Modifier.fillMaxWidth()
		)
		TextInput(
			state = input.content,
			hint = "活动内容",
			maxLength = 512,
			maxLines = 10,
			modifier = Modifier.fillMaxWidth()
		)
		TextInput(
			state = input.showstart,
			hint = "秀动ID(可空)",
			maxLength = 1024,
			maxLines = 5,
			modifier = Modifier.fillMaxWidth()
		)
		TextInput(
			state = input.damai,
			hint = "大麦ID(可空)",
			maxLength = 16,
			modifier = Modifier.fillMaxWidth()
		)
		TextInput(
			state = input.maoyan,
			hint = "猫眼ID(可空)",
			maxLength = 16,
			modifier = Modifier.fillMaxWidth()
		)
		TextInput(
			state = input.link,
			hint = "活动链接(可空)",
			maxLength = 256,
			modifier = Modifier.fillMaxWidth()
		)
		if (content != null) content()
		Text(
			text = "轮播图(可空)",
			style = MaterialTheme.typography.titleMedium
		)
		val pic = input.pic
		if (pic == null) {
			Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f).clickable(onClick = {
				picPicker.select()
			})) {
				EmptyBox()
			}
		}
		else {
			Box(
				modifier = Modifier.fillMaxWidth().aspectRatio(2f),
				contentAlignment = Alignment.Center
			) {
				ClickIcon(
					imageVector = Icons.Outlined.Cancel,
					color = MaterialTheme.colorScheme.error,
					size = 32.dp,
					modifier = Modifier.padding(5.dp).align(Alignment.TopEnd).zIndex(2f),
					onClick = { onPicDelete() }
				)
				WebImage(
					uri = pic.image,
					modifier = Modifier.fillMaxSize().zIndex(1f),
					contentScale = ContentScale.Crop,
					onClick = { picPicker.select() }
				)
			}
		}
		Text(
			text = "活动海报",
			style = MaterialTheme.typography.titleMedium
		)
		ImageAdder(
			maxNum = 9,
			pics = input.pics,
			size = 80.dp,
			modifier = Modifier.fillMaxWidth(),
			onAdd = { picsPicker.select() },
			onDelete = { onPicsDelete(it) },
			onClick = { onPicsClick(input.pics, it) }
		)
	}

	if (input.cropState.isOpen) {
		DialogCrop(
			state = input.cropState,
			aspectRatio = 2f,
			onCropped = { onPicCrop(it) }
		)
	}
}