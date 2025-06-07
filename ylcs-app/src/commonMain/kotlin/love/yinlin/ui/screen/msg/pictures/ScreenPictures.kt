package love.yinlin.ui.screen.msg.pictures

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastMap
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import love.yinlin.AppModel
import love.yinlin.api.ClientAPI
import love.yinlin.api.ServerRes
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.extension.String
import love.yinlin.platform.Coroutines
import love.yinlin.resources.Res
import love.yinlin.resources.img_photo_album
import love.yinlin.ui.component.container.Breadcrumb
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.CommonSubScreen
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

@Composable
private fun PhotoFile(
    item: PhotoItem.File,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shadowElevation = ThemeValue.Shadow.Surface
    ) {
        WebImage(
            uri = item.thumb,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onClick = onClick
        )
    }
}

@Composable
private fun PhotoFolderText(
    text: String,
    color: Color = Colors.Unspecified,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun PhotoFolder(
    item: PhotoItem.Folder,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isAlbum = remember(item) { item.items.isNotEmpty() && item.items.fastAll { it is PhotoItem.File } }

    Surface(
        modifier = modifier,
        shadowElevation = ThemeValue.Shadow.Surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isAlbum) {
                WebImage(
                    uri = (item.items.first() as PhotoItem.File).thumb,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
            else {
                MiniImage(
                    res = Res.drawable.img_photo_album,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
            PhotoFolderText(
                text = item.name,
                modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value)
            )
        }
    }
}

@Stable
class ScreenPictures(model: AppModel) : CommonSubScreen(model) {
    private var photos by mutableStateOf(PhotoItem.Home)
    private var stack = mutableStateListOf(photos)
    private var state by mutableStateOf(BoxState.EMPTY)
    private val listState = LazyGridState()
    private val current by derivedStateOf { stack.last() }

    private suspend fun loadPhotos() {
        if (state != BoxState.LOADING) {
            state = BoxState.LOADING
            val result = ClientAPI.request<JsonObject>(route = ServerRes.Photo)
            val data = Coroutines.cpu {
                try {
                    PhotoItem.parseJson("相册", (result as Data.Success).data)
                } catch (_: Throwable) {
                    null
                }
            }
            if (data != null) {
                photos = data
                stack.clear()
                stack += photos
                state = BoxState.CONTENT
            }
            else state = BoxState.NETWORK_ERROR
        }
    }

    override val title: String = "美图"

    override fun onBack() {
        if (stack.size > 1) stack.removeLastOrNull()
        else pop()
    }

    override suspend fun initialize() {
        loadPhotos()
    }

    @Composable
    override fun SubContent(device: Device) {
        LaunchedEffect(stack.size) {
            listState.scrollToItem(0)
        }

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
                        }
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(ThemeValue.Size.CellWidth),
                    state = listState,
                    contentPadding = ThemeValue.Padding.EqualValue,
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                    verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    itemsIndexed(
                        items = current.items,
                        key = { _, item -> item.name }
                    ) { index, item ->
                        when (item) {
                            is PhotoItem.File -> PhotoFile(
                                item = item,
                                modifier = Modifier.fillMaxWidth().aspectRatio(0.66667f),
                                onClick = {
                                    val pics = current.items.fastMap {
                                        val file = it as PhotoItem.File
                                        Picture(file.thumb, file.source)
                                    }
                                    navigate(ScreenImagePreview.Args(pics, index))
                                }
                            )
                            is PhotoItem.Folder -> PhotoFolder(
                                item = item,
                                modifier = Modifier.fillMaxWidth().aspectRatio(0.66667f),
                                onClick = { stack += item }
                            )
                        }
                    }
                }
            }
        }
    }

    private val isScrollTop: Boolean by derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }

    override val fabCanExpand: Boolean = false

    override val fabIcon: ImageVector get() = if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward

    override suspend fun onFabClick() {
        if (isScrollTop) launch { loadPhotos() }
        else listState.animateScrollToItem(0)
    }
}