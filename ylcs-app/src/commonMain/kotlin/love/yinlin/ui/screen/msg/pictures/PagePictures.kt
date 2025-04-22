package love.yinlin.ui.screen.msg.pictures

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.data.common.Picture
import love.yinlin.extension.LaunchOnce
import love.yinlin.extension.rememberDerivedState
import love.yinlin.resources.Res
import love.yinlin.resources.img_photo_album
import love.yinlin.ui.component.container.Breadcrumb
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.screen.common.ScreenImagePreview
import love.yinlin.ui.screen.msg.PhotoItem
import love.yinlin.ui.screen.msg.ScreenPartMsg
import org.jetbrains.compose.resources.painterResource

@Composable
private fun PhotoFile(
    item: PhotoItem.File,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shadowElevation = 5.dp
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
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomStart
    ) {
        repeat(items.size.coerceAtMost(4)) { index ->
            WebImage(
                uri = items[index].thumb,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(start = 40.dp * index)
                    .fillMaxSize()
                    .zIndex(index.toFloat())
            )
        }
    }
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
        shadowElevation = 5.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isAlbum) {
                CombinedPhotoFolder(
                    items = item.items.map { it as PhotoItem.File },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
            else {
                Image(
                    painter = painterResource(Res.drawable.img_photo_album),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
            Text(
                text = item.name,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(10.dp)
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
                tonalElevation = 1.dp,
                shadowElevation = 5.dp
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
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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