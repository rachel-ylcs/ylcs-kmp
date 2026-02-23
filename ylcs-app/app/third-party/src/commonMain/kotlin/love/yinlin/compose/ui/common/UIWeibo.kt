package love.yinlin.compose.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.common.DataSourceWeibo
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.DialogDownload
import love.yinlin.compose.ui.floating.downloadPhotos
import love.yinlin.compose.ui.floating.downloadVideo
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.NineGrid
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.text.RachelRichText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.coroutines.ioContext
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.platform.Platform
import love.yinlin.platform.UnsupportedPlatformText

@Composable
fun BasicScreen.WeiboUserBar(
    info: WeiboUserInfo,
    time: String,
    location: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
    ) {
        WebImage(
            uri = info.avatar,
            key = remember { DateEx.TodayString },
            contentScale = ContentScale.Crop,
            circle = true,
            modifier = Modifier.fillMaxHeight().aspectRatio(1f),
            onClick = {
                with(DataSourceWeibo.processor) { onWeiboAvatarClick(info) }
            }
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            SimpleEllipsisText(
                modifier = Modifier.fillMaxWidth(),
                text = info.name,
                color = Theme.color.primary,
                style = Theme.typography.v7.bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SimpleEllipsisText(
                    text = time,
                    style = Theme.typography.v8,
                    color = LocalColorVariant.current
                )
                SimpleEllipsisText(
                    text = location,
                    style = Theme.typography.v8,
                    color = LocalColorVariant.current,
                )
            }
        }
    }
}

@Composable
fun BasicScreen.WeiboLayout(
    weibo: Weibo,
    downloadDialog: DialogDownload?,
) {
    WeiboUserBar(
        info = weibo.info,
        time = weibo.timeString,
        location = weibo.location
    )

    RachelRichText(
        text = weibo.text,
        modifier = Modifier.fillMaxWidth(),
        overflow = TextOverflow.Ellipsis,
        onLinkClick = {
            with(DataSourceWeibo.processor) { onWeiboLinkClick(it) }
        },
        onTopicClick = {
            with(DataSourceWeibo.processor) { onWeiboTopicClick(it) }
        },
        onAtClick = {
            with(DataSourceWeibo.processor) { onWeiboAtClick(it) }
        }
    )

    val hasMediaResource = weibo.pictures.isNotEmpty()

    if (hasMediaResource) {
        NineGrid(
            pics = weibo.pictures,
            modifier = Modifier.fillMaxWidth(),
            onImageClick = { index, _ ->
                with(DataSourceWeibo.processor) { onWeiboPicClick(weibo.pictures, index) }
            },
            onVideoClick = {
                with(DataSourceWeibo.processor) { onWeiboVideoClick(it) }
            }
        ) { isSingle, pic, onClick ->
            WebImage(
                uri = pic.image,
                modifier = Modifier.fillMaxWidth().condition(isSingle, ifTrue = { aspectRatio(1.77778f) }, ifFalse = { fillMaxHeight() }),
                contentScale = if (isSingle) ContentScale.Inside else ContentScale.Crop,
                onClick = onClick
            )
        }
    }

    // Bottom Bar
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TextIconAdapter { idIcon, idText ->
            Icon(icon = Icons.ThumbUp, modifier = Modifier.idIcon())
            SimpleEllipsisText(text = weibo.likeNum.toString(), modifier = Modifier.idText())
        }
        TextIconAdapter { idIcon, idText ->
            Icon(icon = Icons.Comment, modifier = Modifier.idIcon())
            SimpleEllipsisText(text = weibo.commentNum.toString(), modifier = Modifier.idText())
        }
        TextIconAdapter { idIcon, idText ->
            Icon(icon = Icons.Share, modifier = Modifier.idIcon())
            SimpleEllipsisText(text = weibo.repostNum.toString(), modifier = Modifier.idText())
        }

        if (hasMediaResource && downloadDialog != null) {
            Icon(
                icon = Icons.Download,
                onClick = {
                    val video = weibo.pictures.find { it.isVideo }?.video
                    if (video != null) {
                        launch(ioContext) {
                            downloadDialog.downloadVideo(video)
                        }
                    }
                    else {
                        Platform.use(
                            *Platform.Phone,
                            ifTrue = {
                                launch(ioContext) {
                                    downloadDialog.downloadPhotos(weibo.pictures.map { it.source })
                                }
                            },
                            ifFalse = { slot.tip.warning(UnsupportedPlatformText) }
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun BasicScreen.WeiboCard(
    weibo: Weibo,
    modifier: Modifier = Modifier,
    downloadDialog: DialogDownload?,
) {
    Surface(
        modifier = modifier,
        shape = Theme.shape.v3,
        contentPadding = Theme.padding.eValue,
        shadowElevation = Theme.shadow.v3,
        onClick = {
            with(DataSourceWeibo.processor) { onWeiboClick(weibo) }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v10)
        ) {
            WeiboLayout(weibo = weibo, downloadDialog = downloadDialog)
        }
    }
}

@Composable
fun BasicScreen.WeiboGrid(
    state: LazyStaggeredGridState,
    items: List<Weibo>,
    modifier: Modifier = Modifier,
    downloadDialog: DialogDownload?,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(Theme.size.cell1),
        modifier = modifier,
        state = state,
        contentPadding = Theme.padding.eValue,
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
        verticalItemSpacing = Theme.padding.e
    ) {
        items(
            items = items,
            key = { it.id }
        ) { weibo ->
            WeiboCard(weibo = weibo, modifier = Modifier.fillMaxWidth(), downloadDialog = downloadDialog)
        }
    }
}