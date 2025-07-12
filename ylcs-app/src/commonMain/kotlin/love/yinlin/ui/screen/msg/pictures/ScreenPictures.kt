package love.yinlin.ui.screen.msg.pictures

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastMap
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import love.yinlin.AppModel
import love.yinlin.api.ClientAPI
import love.yinlin.api.ServerRes
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.common.Picture
import love.yinlin.extension.String
import love.yinlin.extension.catchingNull
import love.yinlin.platform.Coroutines
import love.yinlin.resources.Res
import love.yinlin.resources.img_photo_album
import love.yinlin.ui.component.container.Breadcrumb
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.node.condition
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.screen.FloatingDialogInput
import love.yinlin.ui.screen.common.ScreenImagePreview

@Stable
private sealed class PhotoItem(val name: String) {
    override fun toString(): String = name

    @Stable
    class File(name: String) : PhotoItem(name) {
        val thumb: String = "https://img.picgo.net/$name.md.webp"
        val source: String = "https://img.picgo.net/$name.webp"
    }

    @Stable
    class Folder (name: String, val items: List<PhotoItem>) : PhotoItem(name)

    companion object {
        val Home: Folder = Folder("相册", emptyList())

        fun parseJson(name: String, json: JsonObject): Folder {
            val items = mutableListOf<PhotoItem>()
            val folder = Folder(name, items)
            for ((key, value) in json) {
                items += when (value) {
                    is JsonObject -> parseJson(key, value)
                    is JsonArray -> Folder(key, value.fastMap { File(it.String) })
                    else -> error("")
                }
            }
            return folder
        }
    }
}

@Stable
class ScreenPictures(model: AppModel) : CommonSubScreen(model) {
    private var photos by mutableStateOf(PhotoItem.Home)
    private var stack = mutableStateListOf(photos)
    private var state: BoxState by mutableStateOf(EMPTY)
    private val listState = LazyListState()
    private val current by derivedStateOf { stack.last() }

    private suspend fun loadPhotos() {
        if (state != LOADING) {
            state = LOADING
            val result = ClientAPI.request<JsonObject>(route = ServerRes.Photo)
            val data = Coroutines.cpu { catchingNull { PhotoItem.parseJson("相册", (result as Success).data) } }
            if (data != null) {
                photos = data
                stack.clear()
                stack += photos
                state = CONTENT
            }
            else state = NETWORK_ERROR
        }
    }

    private fun searchFolderItem(virtualStack: MutableList<PhotoItem.Folder>, index: Int, folder: PhotoItem.Folder, key: String): Int? {
        if (folder.name.contains(key, true)) return index
        else {
            for ((i, item) in folder.items.withIndex()) {
                if (item is Folder) {
                    virtualStack.add(item)
                    val fetchIndex = searchFolderItem(virtualStack, i, item, key)
                    if (fetchIndex != null) return fetchIndex
                    virtualStack.removeLast()
                }
                else return null
            }
            return null
        }
    }

    private suspend fun searchFolder(key: String) {
        val virtualStack = mutableListOf(photos)
        val result = Coroutines.cpu { searchFolderItem(virtualStack, 0, photos, key) }
        if (result != null) {
            stack.clear()
            val isAlbum = virtualStack.last().items.all { it is File }
            if (isAlbum) virtualStack.removeLast()
            for (step in virtualStack) stack += step
            if (isAlbum) listState.animateScrollToItem(result)
        }
        else slot.tip.warning("未找到相关图集")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PhotoList(
        folder: PhotoItem.Folder,
        modifier: Modifier = Modifier,
        onAlbumClick: (PhotoItem.Folder, Int) -> Unit,
        onEnterFolder: (PhotoItem.Folder) -> Unit,
    ) {
        LazyColumn(
            state = listState,
            contentPadding = ThemeValue.Padding.EqualValue,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
            modifier = modifier
        ) {
            items(
                items = folder.items,
                key = { it.name }
            ) { item ->
                if (item is Folder) {
                    val isAlbum = remember(item) { item.items.isNotEmpty() && item.items.fastAll { it is File } }
                    if (isAlbum) {
                        HorizontalMultiBrowseCarousel(
                            state = rememberCarouselState { item.items.size },
                            preferredItemWidth = ThemeValue.Size.CellWidth,
                            itemSpacing = ThemeValue.Padding.HorizontalSpace,
                            modifier = Modifier.fillMaxWidth().clipToBounds()
                        ) { index ->
                            WebImage(
                                uri = (item.items[index] as File).thumb,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.width(ThemeValue.Size.CellWidth)
                                    .aspectRatio(0.66667f)
                                    .maskClip(MaterialTheme.shapes.large),
                                onClick = { onAlbumClick(item, index) }
                            )
                        }
                    }
                    else {
                        MiniImage(
                            res = Res.drawable.img_photo_album,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.width(ThemeValue.Size.CellWidth)
                                .aspectRatio(0.66667f)
                                .shadow(ThemeValue.Shadow.Surface, MaterialTheme.shapes.large)
                                .clip(MaterialTheme.shapes.large)
                                .clickable { onEnterFolder(item) },
                        )
                    }

                    Spacer(modifier = Modifier.height(ThemeValue.Padding.VerticalExtraSpace))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = Center,
                        maxLines = 1,
                        overflow = Ellipsis,
                        modifier = Modifier.condition(isAlbum, ifTrue = { fillMaxWidth() }, ifFalse = { width(ThemeValue.Size.CellWidth) })
                    )
                }
            }
        }
    }

    override val title: String = "美图"

    override fun onBack() {
        if (stack.size > 1) {
            stack.removeLastOrNull()
            listState.requestScrollToItem(0)
        }
        else pop()
    }

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(Icons.Outlined.EmojiObjects, "分享") {
            slot.info.openSuspend(
                title = "图集分享",
                content = "欢迎大家分享相机线下拍摄的超清原图给其他小银子, 有贡献图集意愿的小银子可联系我们收录, 我们也会为您赠予银币奖励!"
            )
        }

        ActionSuspend(Icons.Outlined.Search, "搜索") {
            val result = searchDialog.openSuspend()
            if (result != null) searchFolder(result)
        }
    }

    override suspend fun initialize() {
        loadPhotos()
    }

    @Composable
    override fun SubContent(device: Device) {
        StatefulBox(
            state = state,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = ThemeValue.Shadow.Tonal,
                    shadowElevation = ThemeValue.Shadow.Surface
                ) {
                    Breadcrumb(
                        items = stack,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            stack.removeRange(it + 1, stack.size)
                            listState.requestScrollToItem(0)
                        }
                    )
                }
                PhotoList(
                    folder = current,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    onAlbumClick = { folder, index ->
                        val pics = folder.items.fastMap {
                            val file = it as File
                            Picture(file.thumb, file.source)
                        }
                        navigate(ScreenImagePreview.Args(pics, index))
                    },
                    onEnterFolder = {
                        stack += it
                        listState.requestScrollToItem(0)
                    }
                )
            }
        }
    }

    private val isScrollTop: Boolean by derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector get() = if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward

    override suspend fun onFabClick() {
        if (isScrollTop) launch { loadPhotos() }
        else listState.animateScrollToItem(0)
    }

    private val searchDialog = FloatingDialogInput(
        hint = "分类名",
        maxLength = 32
    )

    @Composable
    override fun Floating() {
        searchDialog.Land()
    }
}