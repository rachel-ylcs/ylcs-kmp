package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.container.HorizontalScrollContainer
import love.yinlin.compose.ui.container.OverlayAction
import love.yinlin.compose.ui.container.OverlayTopBar
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.floating.FABScrollTop
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.HorizontalDivider
import love.yinlin.compose.ui.layout.PaginationArgs
import love.yinlin.compose.ui.layout.PaginationColumn
import love.yinlin.compose.ui.layout.VerticalDivider
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.cs.APIConfig
import love.yinlin.cs.ApiPhotoSearchPhotoAlbums
import love.yinlin.cs.request
import love.yinlin.cs.requestNull
import love.yinlin.cs.url
import love.yinlin.data.compose.Picture
import love.yinlin.data.rachel.photo.PhotoAlbum

@Stable
class ScreenAlbum : BasicScreen() {
    private val provider = RachelStatefulProvider(
        networkErrorHandler = {
            launch { requestNewPhotos() }
        }
    )

    private var keyword: String? = null

    private val page = object : PaginationArgs<PhotoAlbum, Int, String, Int>(
        default = "2099-12-31",
        default1 = Int.MAX_VALUE,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: PhotoAlbum): Int = item.aid
        override fun offset(item: PhotoAlbum): String = item.ts
        override fun arg1(item: PhotoAlbum): Int = item.aid
    }

    private val listState = LazyListState()

    private suspend fun requestNewPhotos(newKeyword: String? = null) {
        keyword = newKeyword
        provider.withLoading {
            val result = ApiPhotoSearchPhotoAlbums.requestNull(newKeyword, page.default, page.default1,page.pageNum)!!
            listState.requestScrollToItem(0)
            page.newData(result.o1)
        }
    }

    private suspend fun requestMorePhotos() {
        ApiPhotoSearchPhotoAlbums.request(keyword, page.offset, page.arg1, page.pageNum) {
            page.moreData(it)
        }
    }

    override suspend fun initialize() {
        requestNewPhotos()
    }

    @Composable
    private fun PhotoAlbumLayout(album: PhotoAlbum, modifier: Modifier = Modifier) {
        if (album.picNum in 1 .. PhotoAlbum.MAX_NUM) {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleEllipsisText(
                        text = album.title,
                        style = Theme.typography.v6.bold,
                        color = Theme.color.primary
                    )
                    SimpleEllipsisText(
                        text = album.picNum.toString(),
                        style = Theme.typography.v6.bold,
                        color = Theme.color.secondary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleEllipsisText("${album.ts}  ${album.location ?: ""}")
                    SimpleEllipsisText(album.author ?: "")
                }

                val state = rememberLazyListState()

                HorizontalScrollContainer(state = state, modifier = Modifier.fillMaxWidth()) {
                    LazyRow(state = state) {
                        items(album.picNum) { index ->
                            Row(modifier = Modifier.width(Theme.size.cell4).aspectRatio(0.66667f)) {
                                WebImage(
                                    uri = album.thumbPath(index).url,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    onClick = {
                                        val pics = List(album.picNum) {
                                            Picture(album.thumbPath(it).url, album.picPath(it).url)
                                        }
                                        navigate(::ScreenImagePreview, pics, index)
                                    }
                                )
                                if (index != album.picNum - 1) VerticalDivider(Theme.border.v4, Theme.color.tertiary)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun BasicContent() {
        Column(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            OverlayTopBar(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9),
                left = OverlayAction.Sync("返回", Icons.ArrowBack, onClick = ::onBack),
                right = OverlayAction.Async("搜索", Icons.Search) {
                    searchDialog.open()?.let { requestNewPhotos(it) }
                }
            )
            StatefulBox(
                provider = provider,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                PaginationColumn(
                    items = page.items,
                    key = { it.aid },
                    state = listState,
                    canRefresh = true,
                    canLoading = page.canLoading,
                    onRefresh = ::requestNewPhotos,
                    onLoading = ::requestMorePhotos,
                    itemDivider = { HorizontalDivider(Theme.border.v3, Theme.color.secondary) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    PhotoAlbumLayout(album = it, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    override val fab: FAB = FABScrollTop(listState)

    private val searchDialog = this land DialogInput(hint = "关键字/地点/作者", maxLength = 16)
}