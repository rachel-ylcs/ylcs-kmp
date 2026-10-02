package love.yinlin.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.compose.ds.DataSourceInformation
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.floating.downloadPhotos
import love.yinlin.compose.ui.floating.downloadVideo
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.NineGrid
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.text.RachelRichText
import love.yinlin.concurrent.Mutex
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.ioContext
import love.yinlin.data.common.ThumbImage
import love.yinlin.data.information.DataValue
import love.yinlin.data.information.UnifiedData
import love.yinlin.data.information.UnifiedMedia
import love.yinlin.data.information.UnifiedMessage
import love.yinlin.data.information.UnifiedUserInfo
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboData
import love.yinlin.data.weibo.WeiboMedia
import love.yinlin.screen.ScreenImagePreview
import love.yinlin.screen.ScreenVideo
import love.yinlin.screen.ScreenWeiboDetails
import love.yinlin.screen.ScreenWeiboUser
import love.yinlin.screen.navigateScreenWebPage
import love.yinlin.tpl.weibo.WeiboAPI
import love.yinlin.tpl.weibo.WeiboCookie
import love.yinlin.tpl.weibo.weiboHtmlToRichString

@Stable
abstract class BasicWeiboManager : MessageManager<Weibo>() {
    companion object {
        private val searchUserMutex = Mutex()

        private var weiboCookie: WeiboCookie? = null

        suspend fun fetchWeiboCookie(): WeiboCookie {
            val oldCookie = weiboCookie
            if (oldCookie == null) {
                val cookie = WeiboAPI.generateCookie()
                weiboCookie = cookie
                return cookie
            }
            return oldCookie
        }

        internal fun resetWeiboCookies() {
            weiboCookie = null
        }
    }

    override fun BasicScreen.onMessageClick(message: UnifiedMessage) {
        if (message is Weibo) navigate(::ScreenWeiboDetails, message)
    }

    override fun BasicScreen.onAvatarClick(user: UnifiedUserInfo) {
        navigate(::ScreenWeiboUser, user.id)
    }

    @Composable
    final override fun BasicScreen.MessageTextRender(modifier: Modifier, text: String, style: TextStyle) {
        val richString = remember(text) { weiboHtmlToRichString(text) }

        RachelRichText(
            text = richString,
            modifier = modifier,
            overflow = TextOverflow.Ellipsis,
            onLinkClick = { navigateScreenWebPage(it) },
            onTopicClick = { navigateScreenWebPage(it) },
            onAtClick = { arg ->
                val name = arg.substringAfterLast("/n/")
                if (name.isEmpty()) navigateScreenWebPage(name)
                else if (searchUserMutex.tryLock()) {
                    launch {
                        val user = Coroutines.catchingNull {
                            val cookie = fetchWeiboCookie()
                            WeiboAPI.searchUser(name, cookie)?.find { it.name == name }
                        }
                        searchUserMutex.unlock()
                        if (user == null) navigateScreenWebPage(name)
                        else navigate(::ScreenWeiboUser, user.id)
                    }
                }
            }
        )
    }

    @Composable
    override fun BasicScreen.MessageMediaLayout(modifier: Modifier, medias: List<UnifiedMedia>) {
        NineGrid(
            pics = medias,
            modifier = modifier,
            unique = true,
            onImageClick = { index, _ ->
                navigate(::ScreenImagePreview, medias.map {
                    (val image, val source) = it as WeiboMedia.Image
                    ThumbImage(image, source)
                }, index)
            },
            onVideoClick = { media ->
                val video = (media as? WeiboMedia.Video)?.video
                if (video != null) navigate(::ScreenVideo, video)
            }
        ) { contentScale, media, onClick ->
            WebImage(
                uri = media.image,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onClick = onClick
            )
        }
    }

    @Composable
    override fun BasicScreen.MessageDataBar(modifier: Modifier, data: UnifiedData) {
        if (data is WeiboData) {
            MessageDataFlow(
                modifier = modifier,
                values = remember(data) {
                    [
                        DataValue(title = "点赞", value = data.likeNum.toString(), icon = Icons.ThumbUp),
                        DataValue(title = "评论", value = data.commentNum.toString(), icon = Icons.Comment),
                        DataValue(title = "转发", value = data.repostNum.toString(), icon = Icons.Share),
                    ]
                }
            )
        }
    }

    override fun checkExtraData(message: UnifiedMessage): Boolean = message.medias.isNotEmpty()

    @Composable
    override fun BasicScreen.MessageExtraLayout(modifier: Modifier, message: UnifiedMessage) {
        MediaDownloadButton<Weibo>(
            message = message,
            onClick = { weibo ->
                val medias = weibo.medias
                launch(ioContext) {
                    val first = medias[0]
                    if (medias.size == 1 && first is WeiboMedia.Video) DataSourceInformation.CommonDownloadDialog.downloadVideo(first.video)
                    else DataSourceInformation.CommonDownloadDialog.downloadPhotos(medias.map { media ->
                        when (media) {
                            is WeiboMedia.Image -> media.source
                            is WeiboMedia.Video -> media.image
                        }
                    })
                }
            }
        )
    }
}