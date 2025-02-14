package love.yinlin.ui.screen.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import love.yinlin.AppModel
import love.yinlin.app
import love.yinlin.data.common.Picture
import love.yinlin.extension.DateEx
import love.yinlin.extension.condition
import love.yinlin.launch
import love.yinlin.platform.Coroutines
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.image.ZoomWebImage
import love.yinlin.ui.component.screen.DialogProgress
import love.yinlin.ui.component.screen.DialogProgressState
import love.yinlin.ui.component.screen.SubScreen

@Stable
class PreviewPicture(val pic: Picture) {
	var isSource: Boolean by mutableStateOf(false)
}

class ImagePreviewModel(
	images: List<Picture>,
	current: Int
) : ViewModel() {
	val previews: List<PreviewPicture> = images.map { PreviewPicture(it) }
	var current: Int by mutableIntStateOf(current)

	val dialogState = DialogProgressState()
	fun downloadPicture() {
		val preview = previews[current]
		val url = if (preview.isSource) preview.pic.source else preview.pic.image
		launch {
			dialogState.isOpen = true
			dialogState.progress = 0
			dialogState.maxProgress = 100
			Coroutines.io {
				repeat(20) {
					delay(300)
					dialogState.progress += 5
				}
			}
			dialogState.isOpen = false
		}
	}
}

@Composable
private fun PreviewLayout(
	model: ImagePreviewModel,
	current: Int,
	modifier: Modifier = Modifier
) {
	val preview = model.previews[current]

	val isAllSources by derivedStateOf { model.previews.all { it.isSource } }

	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(10.dp),
	) {
		ZoomWebImage(
			uri = if (preview.isSource) preview.pic.source else preview.pic.image,
			key = DateEx.currentDateString,
			modifier = Modifier.fillMaxWidth().weight(1f)
		)
		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(10.dp)
		) {
			Checkbox(
				checked = preview.isSource,
				onCheckedChange = { preview.isSource = it }
			)
			Text(text = "原图")
			Checkbox(
				checked = isAllSources,
				onCheckedChange = { value ->
					model.previews.forEach { it.isSource = value }
				}
			)
			Text(text = if (isAllSources) "取消全选" else "全选")
		}
	}
}

@Composable
private fun Portrait(
	model: ImagePreviewModel
) {

}

@Composable
private fun Landscape(
	model: ImagePreviewModel
) {
	Row(modifier = Modifier.fillMaxSize()) {
		val state = rememberLazyListState()
		LazyColumn(
			modifier = Modifier.width(150.dp).fillMaxHeight(),
			state = state,
			contentPadding = PaddingValues(horizontal = 10.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(10.dp),
		) {
			itemsIndexed(items = model.previews) { index, item ->
				WebImage(
					uri = item.pic.image,
					key = DateEx.currentDateString,
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxWidth().height(150.dp)
						.condition(index == model.current) {
							border(2.dp, MaterialTheme.colorScheme.primary)
						},
					onClick = {
						model.current = index
					}
				)
			}
		}
		VerticalDivider(modifier = Modifier.padding(end = 10.dp))
		PreviewLayout(
			model = model,
			current = model.current,
			modifier = Modifier.weight(1f).fillMaxHeight()
		)

		LaunchedEffect(Unit) {
			state.scrollToItem(model.current)
		}
	}
}

@Composable
fun ScreenImagePreview(model: AppModel, images: List<Picture>, current: Int) {
	val screenModel = viewModel { ImagePreviewModel(
		images = images,
		current = current
	) }

	SubScreen(
		modifier = Modifier.fillMaxSize(),
		title = "${screenModel.current + 1} / ${screenModel.previews.size}",
		actions = {
			ClickIcon(
				imageVector = Icons.Filled.Download,
				modifier = Modifier.padding(end = 5.dp),
				onClick = { screenModel.downloadPicture() }
			)
		},
		onBack = { model.pop() }
	) {
		if (app.isPortrait) Portrait(model = screenModel)
		else Landscape(model = screenModel)
	}

	if (screenModel.dialogState.isOpen) {
		DialogProgress(state = screenModel.dialogState)
	}
}