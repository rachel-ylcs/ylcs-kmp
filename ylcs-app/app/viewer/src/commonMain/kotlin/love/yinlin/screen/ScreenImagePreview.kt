package love.yinlin.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.floating.DialogDownload
import love.yinlin.compose.ui.floating.downloadPhoto
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.image.ZoomWebImage
import love.yinlin.compose.ui.input.CheckBox
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.node.condition
import love.yinlin.coroutines.ioContext
import love.yinlin.data.compose.Picture

@Stable
class ScreenImagePreview(private val images: List<Picture>, initIndex: Int) : Screen() {
    private var downloadSource: Boolean by mutableStateOf(false)
    private val pagerState = PagerState(initIndex) { images.size }

    private fun downloadPicture() {
        val image = images[pagerState.settledPage]
        val url = if (downloadSource) image.source else image.image
        launch(ioContext) { downloadDialog.downloadPhoto(url) }
    }

    override val title: String get() = "${pagerState.settledPage + 1} / ${images.size}"

    @Composable
    override fun RowScope.RightActions() {
        CheckBox(
            checked = downloadSource,
            text = "原图",
            onChecked = { downloadSource = it }
        )
        Icon(icon = Icons.Download, tip = "下载", onClick = { downloadPicture() })
    }

    @Composable
    private fun Portrait() {
        HorizontalPager(
            state = pagerState,
            key = { images[it].image },
            beyondViewportPageCount = 1,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            ZoomWebImage(uri = images[it].image, modifier = Modifier.fillMaxSize())
        }
    }

    @Composable
    private fun Landscape() {
        Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            val state = rememberLazyListState(pagerState.settledPage)

            LazyColumn(
                modifier = Modifier.width(Theme.size.image4).fillMaxHeight(),
                state = state
            ) {
                itemsIndexed(items = images) { index, item ->
                    WebImage(
                        uri = item.image,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).condition(index == pagerState.settledPage) {
                            border(Theme.border.v5, Theme.color.primary)
                        },
                        onClick = { pagerState.requestScrollToPage(index) }
                    )
                }
            }
            Divider()
            ZoomWebImage(uri = images[pagerState.settledPage].image, modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }

    @Composable
    override fun Content() {
        when (LocalDevice.current.type) {
            Device.Type.PORTRAIT -> Portrait()
            Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape()
        }
    }

    private val downloadDialog = this land DialogDownload()
}