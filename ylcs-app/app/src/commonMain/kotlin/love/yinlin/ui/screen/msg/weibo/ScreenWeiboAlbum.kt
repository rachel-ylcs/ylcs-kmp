package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LastPage
import androidx.compose.material.icons.outlined.FirstPage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.compose.*
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.common.ScreenImagePreview

@Stable
class ScreenWeiboAlbum(model: AppModel, private val args: Args) : SubScreen<ScreenWeiboAlbum.Args>(model) {
    @Stable
    @Serializable
    data class Args(val containerId: String, val title: String)

    private data class AlbumCache(val count: Int, val items: List<Picture>)

    companion object {
        private const val PIC_LIMIT = 24
        private const val PIC_MAX_LIMIT = 1000
    }

    private var state by mutableStateOf(BoxState.EMPTY)

    private val caches = MutableList<AlbumCache?>(PIC_MAX_LIMIT) { null }
    private var num by mutableIntStateOf(0)
    private var current by mutableIntStateOf(0)
    private var maxNum = 0

    private suspend fun requestAlbum(page: Int) {
        if (caches[page] == null) { // 无缓存
            state = BoxState.LOADING
            val result = WeiboAPI.getWeiboAlbumPics(args.containerId, page, PIC_LIMIT)
            if (result is Data.Success) {
                val (data, count) = result.data
                caches[page] = AlbumCache(count, data)
            }
            state = BoxState.CONTENT
        }
        val currentAlbum = caches[page]
        if (currentAlbum != null) {
            num = currentAlbum.count
            current = page
        }
        else maxNum = page - 1
    }

    private fun onPrevious() {
        if (state != BoxState.LOADING) {
            if (current > 1) {
                launch { requestAlbum(current - 1) }
            }
            else slot.tip.warning("已经是第一页啦")
        }
    }

    private fun onNext() {
        if (state != BoxState.LOADING) {
            if ((maxNum == 0 || current < maxNum) && current < PIC_MAX_LIMIT - 2) {
                launch { requestAlbum(current + 1) }
            }
            else slot.tip.warning("已经是最后一页啦")
        }
    }

    override suspend fun initialize() {
        requestAlbum(1)
    }

    override val title: String by derivedStateOf { "${args.title} - 共 $num 张" }

    @Composable
    override fun SubContent(device: Device) {
        Column(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(CustomTheme.padding.value),
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace),
        ) {
            val data = caches[current]
            StatefulBox(
                state = state,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                if (data != null) LazyVerticalGrid(
                    columns = GridCells.Adaptive(CustomTheme.size.microCellWidth),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = data.items,
                        key = { _, pic -> pic.image }
                    ){ index, pic ->
                        WebImage(
                            uri = pic.image,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            onClick = { navigate(ScreenImagePreview.Args(data.items, index)) }
                        )
                    }
                }
            }
            if (data != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClickIcon(
                        icon = Icons.Outlined.FirstPage,
                        onClick = { onPrevious() }
                    )
                    Text(
                        text = "第 $current 页",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    ClickIcon(
                        icon = Icons.AutoMirrored.Outlined.LastPage,
                        onClick = { onNext() }
                    )
                }
            }
        }
    }
}