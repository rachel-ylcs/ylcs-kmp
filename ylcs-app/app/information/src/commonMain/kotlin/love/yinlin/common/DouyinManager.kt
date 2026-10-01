package love.yinlin.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.util.fastJoinToString
import kotlinx.coroutines.delay
import love.yinlin.compose.ds.DataSourceInformation
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.NineGrid
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.data.compose.Picture
import love.yinlin.data.douyin.Douyin
import love.yinlin.data.douyin.DouyinData
import love.yinlin.data.douyin.DouyinMedia
import love.yinlin.data.douyin.DouyinUserInfo
import love.yinlin.data.information.DataValue
import love.yinlin.data.information.UnifiedData
import love.yinlin.data.information.UnifiedMedia
import love.yinlin.data.information.UnifiedMessage
import love.yinlin.screen.ScreenDouyinSettings
import love.yinlin.screen.ScreenImagePreview
import love.yinlin.screen.ScreenVideo
import love.yinlin.tpl.douyin.DouyinAPI
import kotlin.time.Duration.Companion.seconds

@Stable
class DouyinManager : MessageManager<Douyin>() {
    override val name: String = "抖音"
    override val icon: ImageVector = Icons2.Douyin
    override val level: APILevel = APILevel.Alpha

    override suspend fun onNewData(flushContent: () -> Unit): Boolean {
        val cookie = DouyinAPI.generateCookie()
        val rachel = DouyinUserInfo.Default[0].id // 默认先银临

        var result = DouyinAPI.requestUserDouyin(rachel, cookie)
        while (result.isNullOrEmpty()) { // 抖音会经常被网关拦住
            delay(1.seconds)
            result = DouyinAPI.requestUserDouyin(rachel, cookie)
        }
        items = result.sortedDescending()

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
                    is DouyinMedia.Video -> Picture(media.cover, video = media.video.fastJoinToString("||"))
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
                val mediaList = pic.video.split("||")
                when (mediaList.size) {
                    0 -> { }
                    1 -> navigate(::ScreenVideo, mediaList[0])
                    else -> {
                        launch {
                            val index = DataSourceInformation.ChoiceDialog.openSuspend(List(mediaList.size) { index -> "线路${index + 1}" })
                            if (index != null) navigate(::ScreenVideo, mediaList[index])
                        }
                    }
                }
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
            MessageDataFlow(
                modifier = modifier,
                values = remember(data) {
                    [
                        DataValue(title = "点赞", value = data.likeNum.toString(), icon = Icons.ThumbUp),
                        DataValue(title = "评论", value = data.commentNum.toString(), icon = Icons.Comment),
                        DataValue(title = "转发", value = data.repostNum.toString(), icon = Icons.Share),
                        DataValue(title = "收藏", value = data.collectNum.toString(), icon = Icons.Favorite),
                        DataValue(title = "推荐", value = data.recommendNum.toString(), icon = Icons.Star),
                        DataValue(title = "打赏", value = data.admireNum.toString(), icon = Icons.Diamond),
                    ]
                }
            )
        }
    }

    override fun checkExtraData(message: UnifiedMessage): Boolean = message.medias.isNotEmpty()

    @Composable
    override fun BasicScreen.MessageExtraLayout(modifier: Modifier, message: UnifiedMessage) {
        MediaDownloadButton(message = message)
    }
}