package love.yinlin.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.floating.DialogDownload
import love.yinlin.compose.ui.floating.downloadPhotos
import love.yinlin.compose.ui.floating.downloadVideo
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.text.RachelRichText
import love.yinlin.coroutines.ioContext
import love.yinlin.data.information.UnifiedMessage
import love.yinlin.data.information.UnifiedUserInfo
import love.yinlin.data.weibo.Weibo
import love.yinlin.screen.ScreenWeiboUser
import love.yinlin.tpl.weibo.weiboHtmlToRichString

@Stable
abstract class BasicWeiboManager : MessageManager<Weibo>() {
    companion object {
        // 公共下载窗口
        // 可通过 Land 落地任何微博相关页面
        val CommonDownloadDialog = DialogDownload()
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
            onLinkClick = {
                // with(DataSourceWeibo.processor) { onWeiboLinkClick(it) }
            },
            onTopicClick = {
                // with(DataSourceWeibo.processor) { onWeiboTopicClick(it) }
            },
            onAtClick = {
                // with(DataSourceWeibo.processor) { onWeiboAtClick(it) }
            }
        )
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