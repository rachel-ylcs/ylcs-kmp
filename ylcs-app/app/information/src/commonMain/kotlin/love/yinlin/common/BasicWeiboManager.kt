package love.yinlin.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.text.RachelRichText
import love.yinlin.data.information.UnifiedMessage
import love.yinlin.data.information.UnifiedUserInfo
import love.yinlin.data.weibo.Weibo
import love.yinlin.tpl.weibo.weiboHtmlToRichString

@Stable
abstract class BasicWeiboManager : MessageManager<Weibo>() {
    override fun BasicScreen.onAvatarClick(user: UnifiedUserInfo) {
        // navigate(::ScreenWeiboUser, user.id)
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
//                val video = weibo.pictures.find { it.isVideo }?.video
//                if (video != null) {
//                    launch(ioContext) {
//                        downloadDialog.downloadVideo(video)
//                    }
//                }
//                else {
//                    Platform.use(
//                        *Platform.Phone,
//                        ifTrue = {
//                            launch(ioContext) {
//                                downloadDialog.downloadPhotos(weibo.pictures.map { it.source })
//                            }
//                        },
//                        ifFalse = { slot.tip.warning(UnsupportedPlatformText) }
//                    )
//                }
            }
        )
    }
}