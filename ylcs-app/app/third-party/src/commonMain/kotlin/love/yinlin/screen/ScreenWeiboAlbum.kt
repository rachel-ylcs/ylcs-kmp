package love.yinlin.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.data.compose.Picture
import love.yinlin.tpl.WeiboAPI

@Stable
class ScreenWeiboAlbum(private val containerId: String, private val albumTitle: String) : Screen() {
    @Stable
    private data class AlbumCache(val count: Int, val items: List<Picture>)

    companion object {
        private const val PIC_LIMIT = 24
        private const val PIC_MAX_LIMIT = 1000
    }

    private val provider = RachelStatefulProvider()

    private val caches = MutableList<AlbumCache?>(PIC_MAX_LIMIT) { null }
    private var num by mutableIntStateOf(0)
    private var current by mutableIntStateOf(0)
    private var maxNum = 0

    private suspend fun requestAlbum(page: Int) {
        if (caches[page] == null) { // 无缓存
            provider.withLoading {
                val (data, count) = WeiboAPI.getWeiboAlbumPics(containerId, page, PIC_LIMIT)!!
                caches[page] = AlbumCache(count, data)
                true
            }
        }
        val currentAlbum = caches[page]
        if (currentAlbum != null) {
            num = currentAlbum.count
            current = page
        }
        else maxNum = page - 1
    }

    private fun onPrevious() {
        if (!provider.isLoading) {
            if (current > 1) {
                launch { requestAlbum(current - 1) }
            }
            else slot.tip.warning("已经是第一页啦")
        }
    }

    private fun onNext() {
        if (!provider.isLoading) {
            if ((maxNum == 0 || current < maxNum) && current < PIC_MAX_LIMIT - 2) {
                launch { requestAlbum(current + 1) }
            }
            else slot.tip.warning("已经是最后一页啦")
        }
    }

    override val title: String get() = "$albumTitle - 共 $num 张"

    override suspend fun initialize() {
        requestAlbum(1)
    }

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(Theme.padding.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
        ) {
            val data = caches[current]
            StatefulBox(
                provider = provider,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                if (data != null) LazyVerticalGrid(
                    columns = GridCells.Adaptive(Theme.size.image5),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = data.items,
                        key = { _, pic -> pic.image }
                    ){ index, pic ->
                        WebImage(
                            uri = pic.image,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            contentScale = ContentScale.Crop,
                            onClick = { navigate(::ScreenImagePreview, data.items, index) }
                        )
                    }
                }
            }

            if (data != null) {
                Row(
                    modifier = Modifier.padding(Theme.padding.value),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon = Icons.FirstPage, onClick = ::onPrevious)
                    SimpleClipText(text = "第 $current 页", style = Theme.typography.v6)
                    Icon(icon = Icons.LastPage, onClick = ::onNext)
                }
            }
        }
    }
}