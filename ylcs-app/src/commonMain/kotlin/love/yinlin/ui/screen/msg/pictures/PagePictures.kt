package love.yinlin.ui.screen.msg.pictures

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import love.yinlin.extension.LaunchOnce
import love.yinlin.extension.rememberDerivedState
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.img_photo_album
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.screen.msg.PhotoItem
import love.yinlin.ui.screen.msg.ScreenPartMsg
import org.jetbrains.compose.resources.painterResource

@Composable
private fun StepItem(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier.wrapContentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

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
                WebImage(
                    uri = remember(item, isAlbum) { (item.items.firstOrNull() as? PhotoItem.File)?.thumb ?: "" },
                    contentScale = ContentScale.Crop,
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

@Composable
private fun Portrait(part: ScreenPartMsg) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = part.photoState.stack) {
                StepItem(text = it.name) {

                }
            }
        }

        val current by rememberDerivedState { part.photoState.stack.last() }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(
                items = current.items,
                key = { it.name }
            ) {
                when (it) {
                    is PhotoItem.File -> PhotoFile(
                        item = it,
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.66667f),
                        onClick = {

                        }
                    )
                    is PhotoItem.Folder -> PhotoFolder(
                        item = it,
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.66667f),
                        onClick = {
                            part.photoState.stack += it
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Landscape(part: ScreenPartMsg) {

}

@Composable
fun ScreenPictures(part: ScreenPartMsg) {
    StatefulBox(
        state = part.photoState.state,
        modifier = Modifier.fillMaxSize()
    ) {
        if (app.isPortrait) Portrait(part)
        else Landscape(part)
    }

    LaunchOnce(part.photoState.flagFirstLoad) {
        part.photoState.loadPhotos()
    }
}