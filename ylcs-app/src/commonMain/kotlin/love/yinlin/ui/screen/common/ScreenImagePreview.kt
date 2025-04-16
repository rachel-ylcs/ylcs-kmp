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
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.data.common.Picture
import love.yinlin.extension.condition
import love.yinlin.extension.fileSizeString
import love.yinlin.platform.PicturePicker
import love.yinlin.platform.app
import love.yinlin.platform.safeDownload
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.image.ZoomWebImage
import love.yinlin.ui.component.screen.DialogProgress
import love.yinlin.ui.component.screen.SubScreen

@Stable
@Serializable
data class ScreenImagePreview(val images: List<Picture>, val index: Int) : Screen<ScreenImagePreview.Model> {
	@Stable
	class PreviewPicture(val pic: Picture) {
		var isSource: Boolean by mutableStateOf(false)
	}

	inner class Model(model: AppModel) : Screen.Model(model) {
		val previews: List<PreviewPicture> = images.map { PreviewPicture(it) }
		var current: Int by mutableIntStateOf(index)

		val downloadDialog = DialogProgress()
		fun downloadPicture() {
			val preview = previews[current]
			val url = if (preview.isSource) preview.pic.source else preview.pic.image
			val filename = url.substringAfterLast('/').substringBefore('?')
			launch {
				PicturePicker.prepareSave(filename)?.let { (origin, sink) ->
					downloadDialog.open()
					val result = sink.use {
						val result = app.fileClient.safeDownload(
							url = url,
							sink = it,
							isCancel = { !downloadDialog.isOpen },
							onGetSize = { total -> downloadDialog.total = total.fileSizeString },
							onTick = { current, total ->
								downloadDialog.current = current.fileSizeString
								if (total != 0L) downloadDialog.progress = current / total.toFloat()
							}
						)
						if (result) PicturePicker.actualSave(filename, origin, sink)
						result
					}
					PicturePicker.cleanSave(origin, result)
					downloadDialog.hide()
				}
			}
		}

		@Composable
		private fun PreviewControls(
			current: Int,
			modifier: Modifier = Modifier
		) {
			val preview = previews[current]
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
		fun Portrait() {
			val state = rememberPagerState(
				initialPage = current,
				pageCount = { previews.size }
			)
			Column(
				modifier = Modifier.fillMaxSize(),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				HorizontalPager(
					state = state,
					key = {
						val preview = previews[it]
						if (preview.isSource) preview.pic.source else preview.pic.image
					},
					modifier = Modifier.fillMaxWidth().weight(1f)
				) {
					val preview = previews[it]
					ZoomWebImage(
						uri = if (preview.isSource) preview.pic.source else preview.pic.image,
						modifier = Modifier.fillMaxSize()
					)
				}
				PreviewControls(
					current = current,
					modifier = Modifier.fillMaxWidth()
				)
			}

			LaunchedEffect(state.currentPage) {
				current = state.currentPage
			}
		}

		@Composable
		fun Landscape() {
			Row(modifier = Modifier.fillMaxSize()) {
				val state = rememberLazyListState(current)
				LazyColumn(
					modifier = Modifier.width(150.dp).fillMaxHeight(),
					state = state,
					contentPadding = PaddingValues(horizontal = 10.dp),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.spacedBy(10.dp),
				) {
					itemsIndexed(items = previews) { index, item ->
						WebImage(
							uri = item.pic.image,
							contentScale = ContentScale.Crop,
							modifier = Modifier.fillMaxWidth().height(150.dp)
								.condition(index == current) {
									border(2.dp, MaterialTheme.colorScheme.primary)
								},
							onClick = {
								current = index
							}
						)
					}
				}
				VerticalDivider(modifier = Modifier.padding(end = 10.dp))
				Column(
					modifier = Modifier.weight(1f).fillMaxHeight(),
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					val preview = previews[current]
					ZoomWebImage(
						uri = if (preview.isSource) preview.pic.source else preview.pic.image,
						modifier = Modifier.fillMaxWidth().weight(1f)
					)
					PreviewControls(
						current = current,
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}
	}

	override fun model(model: AppModel): Model = Model(model)

	@Composable
	override fun content(model: Model) {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = "${model.current + 1} / ${model.previews.size}",
			actions = {
				Action(Icons.Filled.Download) {
					model.downloadPicture()
				}
			},
			onBack = { model.pop() },
			slot = model.slot
		) {
			if (app.isPortrait) model.Portrait()
			else model.Landscape()
		}

		model.downloadDialog.withOpen()
	}
}