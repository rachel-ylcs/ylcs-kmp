package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.github.panpf.sketch.ability.bindPauseLoadWhenScrolling
import love.yinlin.common.ThemeValue
import love.yinlin.data.common.Picture
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.localComposition
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.NineGrid
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.text.RichText

@Stable
interface WeiboProcessor {
    fun onWeiboClick(weibo: Weibo)
    fun onWeiboAvatarClick(info: WeiboUserInfo)
    fun onWeiboLinkClick(arg: String)
    fun onWeiboTopicClick(arg: String)
    fun onWeiboAtClick(arg: String)
    fun onWeiboPicClick(pics: List<Picture>, current: Int)
    fun onWeiboVideoClick(pic: Picture)
}

val LocalWeiboProcessor = localComposition<WeiboProcessor>()

@Composable
private fun WeiboIconValue(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.CenterHorizontally)
    ) {
        MiniIcon(icon = icon, size = ThemeValue.Size.MicroIcon)
        Text(text = text)
    }
}

@Composable
fun WeiboDataBar(
    like: Int,
    comment: Int,
    repost: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace)
    ) {
        WeiboIconValue(
            icon = Icons.Filled.ThumbUp,
            text = like.toString(),
            modifier = Modifier.weight(1f)
        )
        WeiboIconValue(
            icon = Icons.AutoMirrored.Filled.Comment,
            text = comment.toString(),
            modifier = Modifier.weight(1f)
        )
        WeiboIconValue(
            icon = Icons.Filled.Share,
            text = repost.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun WeiboUserBar(
    info: WeiboUserInfo,
    time: String,
    location: String
) {
    val processor = LocalWeiboProcessor.current
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace)
    ) {
        Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
            WebImage(
                uri = info.avatar,
                key = remember { DateEx.TodayString },
                contentScale = ContentScale.Crop,
                circle = true,
                modifier = Modifier.matchParentSize(),
                onClick = { processor.onWeiboAvatarClick(info) }
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = info.name,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun WeiboLayout(
    weibo: Weibo,
    onPicturesDownload: ((List<Picture>) -> Unit)?,
    onVideoDownload: ((String) -> Unit)?
) {
    val processor = LocalWeiboProcessor.current
    WeiboUserBar(
        info = weibo.info,
        time = weibo.timeString,
        location = weibo.location
    )
    Spacer(modifier = Modifier.height(ThemeValue.Padding.VerticalExtraSpace))
    RichText(
        text = weibo.text,
        modifier = Modifier.fillMaxWidth(),
        overflow = TextOverflow.Ellipsis,
        onLinkClick = { processor.onWeiboLinkClick(it) },
        onTopicClick = { processor.onWeiboTopicClick(it) },
        onAtClick = { processor.onWeiboAtClick(it) }
    )
    Spacer(modifier = Modifier.height(ThemeValue.Padding.VerticalExtraSpace))
    if (weibo.pictures.isNotEmpty()) {
        NineGrid(
            pics = weibo.pictures,
            modifier = Modifier.fillMaxWidth(),
            onImageClick = { processor.onWeiboPicClick(weibo.pictures, it) },
            onVideoClick = { processor.onWeiboVideoClick(it) }
        )
        Spacer(modifier = Modifier.height(ThemeValue.Padding.VerticalExtraSpace))
        if (onVideoDownload != null && onPicturesDownload != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val video = remember(weibo) { weibo.pictures.find { it.isVideo }?.video }
                if (video != null) {
                    ClickIcon(
                        icon = Icons.Outlined.Download,
                        onClick = { onVideoDownload(video) }
                    )
                }
                else {
                    ClickIcon(
                        icon = Icons.Outlined.Download,
                        onClick = { onPicturesDownload(weibo.pictures) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(ThemeValue.Padding.VerticalExtraSpace))
    }
    WeiboDataBar(
        like = weibo.likeNum,
        comment = weibo.commentNum,
        repost = weibo.repostNum,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun WeiboCard(
    weibo: Weibo,
    modifier: Modifier = Modifier,
    onPicturesDownload: ((List<Picture>) -> Unit)?,
    onVideoDownload: ((String) -> Unit)?
) {
    val processor = LocalWeiboProcessor.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        shadowElevation = ThemeValue.Shadow.Surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().clickable {
            processor.onWeiboClick(weibo)
        }.padding(ThemeValue.Padding.EqualValue)) {
            WeiboLayout(
                weibo = weibo,
                onPicturesDownload = onPicturesDownload,
                onVideoDownload = onVideoDownload
            )
        }
    }
}

@Composable
fun WeiboGrid(
    state: LazyStaggeredGridState,
    items: List<Weibo>,
    modifier: Modifier = Modifier,
    onPicturesDownload: ((List<Picture>) -> Unit)?,
    onVideoDownload: ((String) -> Unit)?
) {
    bindPauseLoadWhenScrolling(state)
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(ThemeValue.Size.CardWidth),
        modifier = modifier,
        state = state,
        contentPadding = ThemeValue.Padding.EqualValue,
        horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
        verticalItemSpacing = ThemeValue.Padding.EqualSpace
    ) {
        items(
            items = items,
            key = { it.id }
        ) { weibo ->
            WeiboCard(
                weibo = weibo,
                modifier = Modifier.fillMaxWidth(),
                onPicturesDownload = onPicturesDownload,
                onVideoDownload = onVideoDownload
            )
        }
    }
}