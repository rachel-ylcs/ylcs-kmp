package love.yinlin.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.delay
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.NineGrid
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.data.compose.Picture
import love.yinlin.data.douyin.Douyin
import love.yinlin.data.douyin.DouyinData
import love.yinlin.data.douyin.DouyinMedia
import love.yinlin.data.douyin.DouyinUserInfo
import love.yinlin.data.information.UnifiedData
import love.yinlin.data.information.UnifiedMedia
import love.yinlin.data.information.UnifiedMessage
import love.yinlin.screen.ScreenDouyinSettings
import love.yinlin.screen.ScreenImagePreview
import love.yinlin.tpl.douyin.DouyinAPI
import kotlin.time.Duration.Companion.seconds

@Stable
class DouyinManager : MessageManager<Douyin>() {
    override val name: String = "抖音"
    override val icon: ImageVector = Icons2.Douyin

    override suspend fun onNewData(flushContent: () -> Unit): Boolean {
        val cookie = DouyinAPI.generateCookie()
        val rachel = DouyinUserInfo.Default[0].id // 默认先银临

        var result = DouyinAPI.requestUserDouyin(rachel, cookie)
        while (result.isNullOrEmpty()) { // 抖音会经常被网关拦住
            delay(1.seconds)
            result = DouyinAPI.requestUserDouyin(rachel, cookie)
        }
        items = result

        return false
    }

    override suspend fun onMoreData(): Boolean = false // 不支持

    override fun BasicScreen.openSettings() = navigate(::ScreenDouyinSettings)

    @Composable
    override fun BasicScreen.MessageMediaLayout(modifier: Modifier, medias: List<UnifiedMedia>) {
        val pics = remember(medias) {
            medias.map { media ->
                when (media) {
                    is DouyinMedia.Image -> Picture(media.image)
                    is DouyinMedia.Video -> Picture(media.cover, video = media.video[0])
                    else -> Picture("")
                }
            }
        }

        NineGrid(
            pics = pics,
            modifier = modifier,
            unique = true,
            onImageClick = { index, _ ->
                navigate(::ScreenImagePreview, pics, index)
            },
            onVideoClick = { pic ->
                // TODO: 可能需要切换视频源
            }
        ) { contentScale, pic, onClick ->
            WebImage(
                uri = pic.image,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onClick = onClick
            )
        }
    }

    @Composable
    override fun BasicScreen.MessageDataBar(modifier: Modifier, data: UnifiedData) {
        if (data is DouyinData) {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Star, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = data.recommendNum.toString(), modifier = Modifier.idText())
                }
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Comment, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = data.commentNum.toString(), modifier = Modifier.idText())
                }
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.ThumbUp, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = data.likeNum.toString(), modifier = Modifier.idText())
                }
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Diamond, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = data.admireNum.toString(), modifier = Modifier.idText())
                }
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Share, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = data.repostNum.toString(), modifier = Modifier.idText())
                }
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Favorite, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = data.collectNum.toString(), modifier = Modifier.idText())
                }
            }
        }
    }

    override fun checkExtraData(message: UnifiedMessage): Boolean = message.medias.isNotEmpty()

    @Composable
    override fun BasicScreen.MessageExtraLayout(modifier: Modifier, message: UnifiedMessage) {
        MediaDownloadButton(message = message)
    }
}