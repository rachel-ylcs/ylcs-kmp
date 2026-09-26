package love.yinlin.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.compose.ds.DataSourceInformation
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.floating.DialogDownload
import love.yinlin.compose.ui.floating.downloadPhotos
import love.yinlin.compose.ui.floating.downloadVideo
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.text.RachelRichText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.concurrent.Mutex
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.ioContext
import love.yinlin.data.information.UnifiedData
import love.yinlin.data.information.UnifiedMessage
import love.yinlin.data.information.UnifiedUserInfo
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboData
import love.yinlin.screen.ScreenWeiboDetails
import love.yinlin.screen.ScreenWeiboUser
import love.yinlin.screen.navigateScreenWebPage
import love.yinlin.tpl.weibo.WeiboAPI
import love.yinlin.tpl.weibo.weiboHtmlToRichString

@Stable
abstract class BasicWeiboManager : MessageManager<Weibo>() {
    companion object {
        // 公共下载窗口
        // 可通过 Land 落地任何微博相关页面
        val CommonDownloadDialog = DialogDownload()
        private val searchUserMutex = Mutex()
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
                            val cookie = DataSourceInformation.fetchWeiboCookie()
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
    override fun MessageDataBar(modifier: Modifier, data: UnifiedData) {
        if (data is WeiboData) {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.ThumbUp, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = data.likeNum.toString(), modifier = Modifier.idText())
                }
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Comment, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = data.commentNum.toString(), modifier = Modifier.idText())
                }
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Share, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = data.repostNum.toString(), modifier = Modifier.idText())
                }
            }
        }
    }

    override fun checkExtraData(message: UnifiedMessage): Boolean = message.pictures.isNotEmpty()

    @Composable
    override fun BasicScreen.MessageExtraLayout(modifier: Modifier, message: UnifiedMessage) {
        PrimaryTextButton(
            text = "下载",
            icon = Icons.Download,
            onClick = {
                val pictures = message.pictures
                val video = pictures.find { it.isVideo }?.video
                launch(ioContext) {
                    if (video != null) CommonDownloadDialog.downloadVideo(video)
                    else CommonDownloadDialog.downloadPhotos(pictures.map { it.source })
                }
            }
        )
    }
}