package love.yinlin.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.NineGrid
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.text.RachelRichText
import love.yinlin.data.bilibili.Bilibili
import love.yinlin.data.bilibili.BilibiliData
import love.yinlin.data.bilibili.BilibiliUserInfo
import love.yinlin.data.common.ThumbImage
import love.yinlin.data.information.DataValue
import love.yinlin.data.information.UnifiedData
import love.yinlin.data.information.UnifiedMedia
import love.yinlin.data.information.UnifiedMessage
import love.yinlin.screen.ScreenBilibiliSettings
import love.yinlin.screen.ScreenImagePreview
import love.yinlin.screen.navigateScreenWebPage
import love.yinlin.tpl.bilibili.BilibiliAPI
import love.yinlin.tpl.bilibili.BilibiliCookie
import love.yinlin.tpl.bilibili.bilibiliRichTextToRichString

@Stable
class BilibiliManager : MessageManager<Bilibili>() {
    companion object {
        private var bilibiliCookie: BilibiliCookie? = null

        suspend fun fetchBilibiliCookie(): BilibiliCookie? {
            val oldCookie = bilibiliCookie
            if (oldCookie == null) {
                val cookie = BilibiliAPI.generateCookie()
                bilibiliCookie = cookie
                return cookie
            }
            return oldCookie
        }

        private fun resetBilibiliCookies() {
            bilibiliCookie = null
        }
    }

    override val name: String = "哔哩哔哩"
    override val icon: ImageVector = Icons2.Bilibili
    override val level: APILevel = APILevel.Beta

    private var currentOffset: String? = null

    override suspend fun onNewData(flushContent: () -> Unit): Boolean {
        val cookie = fetchBilibiliCookie() ?: return false
        val rachel = BilibiliUserInfo.Default[0].id
        val result = BilibiliAPI.requestUserDynamic(rachel, cookie)
        if (result == null) {
            resetBilibiliCookies()
            return false
        }
        val [newItems, offset] = result
        require(newItems.isNotEmpty()) { resetBilibiliCookies() }
        currentOffset = offset
        items = newItems
        return offset != null
    }

    override suspend fun onMoreData(): Boolean {
        val cookie = fetchBilibiliCookie() ?: return false
        val rachel = BilibiliUserInfo.Default[0].id
        val result = BilibiliAPI.requestUserDynamic(rachel, cookie, currentOffset)
        if (result == null) {
            resetBilibiliCookies()
            return false
        }
        val [newItems, offset] = result
        require(newItems.isNotEmpty()) { resetBilibiliCookies() }
        currentOffset = offset
        items = items + newItems
        return offset != null
    }

    override fun BasicScreen.openSettings() = navigate(::ScreenBilibiliSettings)

    @Composable
    override fun BasicScreen.MessageTextRender(modifier: Modifier, text: String, style: TextStyle) {
        val richString = remember(text) { bilibiliRichTextToRichString(text) }

        RachelRichText(
            text = richString,
            modifier = modifier,
            overflow = TextOverflow.Ellipsis,
            onLinkClick = { navigateScreenWebPage(it) },
            onTopicClick = { navigateScreenWebPage(it) },
            onAtClick = { _ ->

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
                navigate(::ScreenImagePreview, medias.map { ThumbImage(it.image) }, index)
            },
            onVideoClick = {

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
        if (data is BilibiliData) {
            MessageDataFlow(
                modifier = modifier,
                values = remember(data) {
                    [
                        DataValue(title = "点赞", value = data.likeNum.toString(), icon = Icons.ThumbUp),
                        DataValue(title = "评论", value = data.commentNum.toString(), icon = Icons.Comment),
                        DataValue(title = "转发", value = data.repostNum.toString(), icon = Icons.Share),
                    ] + buildList {
                        if (data.danmakuNum != null) add(DataValue(title = "弹幕", value = data.danmakuNum.toString(), icon = Icons.Commit))
                        if (data.playNum != null) add(DataValue(title = "播放", value = data.playNum.toString(), icon = Icons.Play))
                    }
                }
            )
        }
    }

    override fun checkExtraData(message: UnifiedMessage): Boolean = false
}