package love.yinlin.ui.screen.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import love.yinlin.AppModel
import love.yinlin.common.ScreenModel
import love.yinlin.common.screen
import love.yinlin.data.common.Picture
import love.yinlin.extension.condition
import love.yinlin.platform.OS
import love.yinlin.platform.app
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.image.ZoomWebImage
import love.yinlin.ui.component.screen.DialogProgress
import love.yinlin.ui.component.screen.DialogProgressState
import love.yinlin.ui.component.screen.SubScreen

@Stable
private class PreviewPicture(val pic: Picture) {
	var isSource: Boolean by mutableStateOf(false)
}

private class ImagePreviewModel(
	images: List<Picture>,
	current: Int
) : ScreenModel() {
	val previews: List<PreviewPicture> = images.map { PreviewPicture(it) }
	var current: Int by mutableIntStateOf(current)

	val dialogState = DialogProgressState()
	fun downloadPicture() {
		val preview = previews[current]
		val url = if (preview.isSource) preview.pic.source else preview.pic.image
		launch { OS.downloadImage(url, dialogState) }
	}
}

@Composable
private fun PreviewControls(
	model: ImagePreviewModel,
	current: Int,
	modifier: Modifier = Modifier
) {
	val preview = model.previews[current]
	Row(
		modifier = modifier,
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(10.dp)
	) {
		Checkbox(
			checked = preview.isSource,
			onCheckedChange = {
				preview.isSource = it
			}
		)
		Text(text = "原图")
	}
}

@Composable
private fun Portrait(model: ImagePreviewModel) {
	val state = rememberPagerState(
		initialPage = model.current,
		pageCount = { model.previews.size }
	)
	Column(
		modifier = Modifier.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		HorizontalPager(
			state = state,
			key = {
				val preview = model.previews[it]
				if (preview.isSource) preview.pic.source else preview.pic.image
			},
			modifier = Modifier.fillMaxWidth().weight(1f)
		) {
			val preview = model.previews[it]
			ZoomWebImage(
				uri = if (preview.isSource) preview.pic.source else preview.pic.image,
				modifier = Modifier.fillMaxSize()
			)
		}
		PreviewControls(
			model = model,
			current = model.current,
			modifier = Modifier.fillMaxWidth()
		)
	}

	LaunchedEffect(state.currentPage) {
		model.current = state.currentPage
	}
}

@Composable
private fun Landscape(model: ImagePreviewModel) {
	Row(modifier = Modifier.fillMaxSize()) {
		val state = rememberLazyListState(model.current)
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
		Column(
			modifier = Modifier.weight(1f).fillMaxHeight(),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			val preview = model.previews[model.current]
			ZoomWebImage(
				uri = if (preview.isSource) preview.pic.source else preview.pic.image,
				modifier = Modifier.fillMaxWidth().weight(1f)
			)
			PreviewControls(
				model = model,
				current = model.current,
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}

@Composable
fun ScreenImagePreview(model: AppModel, images: List<Picture>, current: Int) {
	val screenModel = screen { ImagePreviewModel(images = images, current = current) }

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