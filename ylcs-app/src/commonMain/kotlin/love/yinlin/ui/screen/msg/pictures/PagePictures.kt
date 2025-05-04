package love.yinlin.ui.screen.msg.pictures

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.common.ThemeValue
import love.yinlin.data.common.Picture
import love.yinlin.extension.LaunchOnce
import love.yinlin.extension.rememberDerivedState
import love.yinlin.resources.Res
import love.yinlin.resources.img_photo_album
import love.yinlin.ui.component.container.Breadcrumb
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.screen.common.ScreenImagePreview
import love.yinlin.ui.screen.msg.PhotoItem
import love.yinlin.ui.screen.msg.ScreenPartMsg

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
private fun CombinedPhotoFolder(
    items: List<PhotoItem.File>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.BottomStart
    ) {
        val subSize = items.size.coerceAtMost(4)
        val gap = if (subSize > 0) maxWidth / subSize else 0.dp
        repeat(subSize) { index ->
            WebImage(
                uri = items[index].thumb,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(start = gap * index)
                    .fillMaxSize()
                    .zIndex(index.toFloat())
            )
        }
    }
}

@Composable
private fun PhotoFolderText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
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
    val isAlbum by rememberDerivedState { item.items.isNotEmpty() && item.items.all { it is PhotoItem.File } }

    Surface(
        modifier = modifier,
        shadowElevation = ThemeValue.Shadow.Surface
    ) {
        if (isAlbum) {
            Column(
                modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CombinedPhotoFolder(
                    items = item.items.map { it as PhotoItem.File },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                PhotoFolderText(
                    text = item.name,
                    modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value)
                )
            }
        }
        else {
            Box(
                modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
                contentAlignment = Alignment.BottomCenter
            ) {
                MiniImage(
                    res = Res.drawable.img_photo_album,
                    modifier = Modifier.fillMaxSize().zIndex(1f)
                )
                PhotoFolderText(
                    text = item.name,
                    modifier = Modifier.fillMaxWidth().padding(bottom = ThemeValue.Padding.VerticalExtraSpace).zIndex(2f)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ScreenPictures(part: ScreenPartMsg) {
    val state = part.photoState
    val current by rememberDerivedState { state.stack.last() }
    val canBack by rememberDerivedState { state.stack.size > 1 }

    BackHandler(canBack) {
        state.stack.removeLastOrNull()
    }

    StatefulBox(
        state = state.state,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = ThemeValue.Shadow.Tonal,
                shadowElevation = ThemeValue.Shadow.Surface
            ) {
                Breadcrumb(
                    items = state.stack,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        state.stack.removeRange(it + 1, state.stack.size)
                    }
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(ThemeValue.Size.CellWidth),
                contentPadding = ThemeValue.Padding.EqualValue,
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                itemsIndexed(
                    items = current.items,
                    key = { index, item -> item.name }
                ) { index, item ->
                    when (item) {
                        is PhotoItem.File -> PhotoFile(
                            item = item,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.66667f),
                            onClick = {
                                val pics = current.items.map {
                                    val file = it as PhotoItem.File
                                    Picture(file.thumb, file.source)
                                }
                                part.navigate(ScreenImagePreview.Args(pics, index))
                            }
                        )
                        is PhotoItem.Folder -> PhotoFolder(
                            item = item,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.66667f),
                            onClick = { state.stack += item }
                        )
                    }
                }
            }
        }
    }

    LaunchOnce(state.flagFirstLoad) {
        state.loadPhotos()
    }
}