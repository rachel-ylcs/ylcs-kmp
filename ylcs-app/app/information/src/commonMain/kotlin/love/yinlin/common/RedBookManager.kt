package love.yinlin.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.NineGrid
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.data.information.DataValue
import love.yinlin.data.information.UnifiedData
import love.yinlin.data.information.UnifiedMedia
import love.yinlin.data.information.UnifiedMessage
import love.yinlin.data.redbook.RedBook
import love.yinlin.data.redbook.RedBookData
import love.yinlin.data.redbook.RedBookUserInfo
import love.yinlin.screen.ScreenRedBookSettings
import love.yinlin.tpl.redbook.RedBookAPI

@Stable
class RedBookManager : MessageManager<RedBook>() {
    override val name: String = "小红书"
    override val icon: ImageVector = Icons2.RedBook
    override val level: APILevel = APILevel.Alpha

    override suspend fun onNewData(flushContent: () -> Unit): Boolean {
        val rachel = RedBookUserInfo.Default[0].id
        items = RedBookAPI.requestUserProfile(rachel) ?: []

        return false
    }

    override suspend fun onMoreData(): Boolean = false

    override fun BasicScreen.openSettings() = navigate(::ScreenRedBookSettings)

    @Composable
    override fun BasicScreen.MessageMediaLayout(modifier: Modifier, medias: List<UnifiedMedia>) {
        NineGrid(
            pics = medias,
            modifier = modifier,
            unique = true,
            onImageClick = { _, _ ->

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
        if (data is RedBookData) {
            MessageDataFlow(
                modifier = modifier,
                values = remember(data) {
                    [
                        DataValue(title = "点赞", value = data.likeNum.toString(), icon = Icons.ThumbUp),
                    ]
                }
            )
        }
    }

    override fun checkExtraData(message: UnifiedMessage): Boolean = false
}