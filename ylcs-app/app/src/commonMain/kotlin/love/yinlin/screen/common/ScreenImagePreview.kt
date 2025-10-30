package love.yinlin.screen.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.ui.util.fastMap
import kotlinx.serialization.Serializable
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.compose.Picture
import love.yinlin.extension.filenameOrRandom
import love.yinlin.platform.Coroutines
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.image.ZoomWebImage
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.floating.FloatingDownloadDialog

@Stable
class ScreenImagePreview(manager: ScreenManager, args: Args) : Screen<ScreenImagePreview.Args>(manager) {
	@Stable
	@Serializable
	data class Args(val images: List<Picture>, val index: Int)

	@Stable
	private class PreviewPicture(val pic: Picture) {
		var isSource: Boolean by mutableStateOf(false)
	}

	private val previews: List<PreviewPicture> = args.images.fastMap { PreviewPicture(it) }
	private var current: Int by mutableIntStateOf(args.index)
	private val pagerState = PagerState(current) { previews.size }

	private fun downloadPicture() {
		val preview = previews[current]
		val url = if (preview.isSource) preview.pic.source else preview.pic.image
		val filename = url.filenameOrRandom(".webp")
		launch {
			Coroutines.io {
				val picker = app.picker
				picker.prepareSavePicture(filename)?.let { (origin, sink) ->
					val result = downloadDialog.openSuspend(url, sink) { picker.actualSave(filename, origin, sink) }
					picker.cleanSave(origin, result)
				}
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
			horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace)
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
	private fun Portrait() {
		Column(
			modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			HorizontalPager(
				state = pagerState,
				key = {
					val preview = previews[it]
					if (preview.isSource) preview.pic.source else preview.pic.image
				},
				beyondViewportPageCount = 1,
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
	}

	@Composable
	private fun Landscape() {
		Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
			val state = rememberLazyListState(current)
			LazyColumn(
				modifier = Modifier.width(CustomTheme.size.largeImage).fillMaxHeight(),
				state = state,
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
			) {
				itemsIndexed(items = previews) { index, item ->
					WebImage(
						uri = item.pic.image,
						contentScale = ContentScale.Crop,
						modifier = Modifier.fillMaxWidth().aspectRatio(1f)
							.condition(index == current) {
								border(CustomTheme.border.medium, MaterialTheme.colorScheme.primary)
							},
						onClick = { current = index }
					)
				}
			}
			VerticalDivider(modifier = Modifier.padding(end = CustomTheme.padding.horizontalSpace))
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

	override val title: String by derivedStateOf { "${current + 1} / ${previews.size}" }

	override suspend fun initialize() {
		monitor(state = { pagerState.settledPage }) { current = it }
	}

	@Composable
	override fun ActionScope.RightActions() {
		Action(Icons.Filled.Download, "下载") {
			downloadPicture()
		}
	}

	@Composable
    override fun Content(device: Device) = when (device.type) {
        Device.Type.PORTRAIT -> Portrait()
        Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape()
    }

	private val downloadDialog = FloatingDownloadDialog()

	@Composable
	override fun Floating() {
		downloadDialog.Land()
	}
}