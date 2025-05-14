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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.common.Colors
import love.yinlin.common.ThemeValue
import love.yinlin.data.common.Picture
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
    val isAlbum = remember(item) { item.items.isNotEmpty() && item.items.all { it is PhotoItem.File } }

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
}