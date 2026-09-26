package love.yinlin.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.NineGrid
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.data.information.UnifiedMessage
import love.yinlin.data.information.UnifiedUserInfo
import love.yinlin.extension.DateEx

@Stable
sealed class MessageManager<T : UnifiedMessage> {
    abstract val name: String // 名称
    abstract val icon: ImageVector // 图标
    abstract suspend fun onNewData(flushContent: () -> Unit): Boolean // 新数据

    abstract fun BasicScreen.openSettings() // 打开设置

    open fun BasicScreen.onAvatarClick(user: UnifiedUserInfo) { } // 点击头像

    var items: List<T> by mutableRefStateOf([])
        protected set
    val gridState = LazyStaggeredGridState()
    var canLoading by mutableStateOf(false)
        protected set

    suspend fun requestNewData(flushContent: () -> Unit): Boolean {
        canLoading = false
        canLoading = onNewData(flushContent)
        gridState.requestScrollToItem(0)
        return items.isNotEmpty()
    }

    suspend fun requestMoreData(flushContent: () -> Unit) {

    }

    @Composable
    open fun BasicScreen.MessageUserLayout(modifier: Modifier, message: UnifiedMessage) {
        val user = message.user

        Row(
            modifier = modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
        ) {
            WebImage(
                uri = user.avatar,
                key = DateEx.TodayLong,
                contentScale = ContentScale.Crop,
                circle = true,
                modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                onClick = { onAvatarClick(user) }
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
            ) {
                SimpleEllipsisText(
                    modifier = Modifier.fillMaxWidth(),
                    text = user.name,
                    color = Theme.color.primary,
                    style = Theme.typography.v7.bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SimpleEllipsisText(
                        text = DateEx.Formatter.standardDateTime.format(message.time) ?: "未知时间",
                        style = Theme.typography.v8,
                        color = LocalColorVariant.current
                    )
                    SimpleEllipsisText(
                        text = message.location,
                        style = Theme.typography.v8,
                        color = LocalColorVariant.current,
                    )
                }
            }
        }
    }

    @Composable
    open fun BasicScreen.MessageTextRender(modifier: Modifier, text: String, style: TextStyle) {
        Text(
            text = text,
            style = style,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }

    @Composable
    private fun MessageDataBar(modifier: Modifier, message: UnifiedMessage) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.ThumbUp, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = message.likeNum.toString(), modifier = Modifier.idText())
            }
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Comment, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = message.commentNum.toString(), modifier = Modifier.idText())
            }
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Share, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = message.repostNum.toString(), modifier = Modifier.idText())
            }
        }
    }

    open fun checkExtraData(message: UnifiedMessage): Boolean = false

    @Composable
    open fun BasicScreen.MessageExtraLayout(modifier: Modifier, message: UnifiedMessage) { }

    @Composable
    fun BasicScreen.MessageLayout(modifier: Modifier, message: UnifiedMessage) {
        Surface(
            modifier = modifier,
            shape = Theme.shape.v3,
            contentPadding = Theme.padding.eValue,
            shadowElevation = Theme.shadow.v3,
            onClick = {

            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                // 用户信息
                MessageUserLayout(modifier = Modifier.fillMaxWidth(), message = message)

                // 标题
                if (message.title.isNotEmpty()) {
                    MessageTextRender(modifier = Modifier.fillMaxWidth(), text = message.title, style = Theme.typography.v6.bold)
                }

                // 内容
                if (message.content.isNotEmpty()) {
                    MessageTextRender(modifier = Modifier.fillMaxWidth(), text = message.content, style = Theme.typography.v7)
                }

                // 图片集
                if (message.pictures.isNotEmpty()) {
                    NineGrid(
                        pics = remember(message) { message.pictures.asPicture },
                        modifier = Modifier.fillMaxWidth(),
                        unique = true,
                        onImageClick = { index, _ ->

                        },
                        onVideoClick = {

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

                // 数据条
                MessageDataBar(modifier = Modifier.fillMaxWidth(), message = message)

                // 拓展栏
                if (checkExtraData(message)) {
                    MessageExtraLayout(modifier = Modifier.fillMaxWidth(), message = message)
                }
            }
        }
    }
}