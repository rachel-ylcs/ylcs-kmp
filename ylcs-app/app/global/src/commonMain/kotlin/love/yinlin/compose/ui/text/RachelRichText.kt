package love.yinlin.compose.ui.text

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.lottie.LottieManager
import love.yinlin.data.rachel.emoji.Emoji
import love.yinlin.data.rachel.emoji.EmojiType
import love.yinlin.foundation.NetClient

@Stable
data object RichEmojiDrawer : RichDrawer {
    private val manager: LottieManager = LottieManager()

    override val type: String = RichType.Emoji.value

    override fun RichRenderScope.render(item: RichObject) = item.cast<RichNodeEmoji> {
        val emojiId = item.id
        val emoji = Emoji.fromId(emojiId) ?: return@cast
        val emojiType = emoji.type
        val size = if (emojiType == EmojiType.Lottie) 1.5f else 3f
        renderCompose(size, size) {
            val emojiPath = emoji.showPath
            if (emojiType == EmojiType.Lottie) {
                val lottieId = emojiId.toString()
                LaunchedEffect(lottieId) {
                    if (manager[lottieId] == null) NetClient.File.download(emojiPath)?.decodeToString()?.let { manager[lottieId] = it }
                }

                manager.Content(lottieId, modifier = Modifier.fillMaxSize())
            }
            else WebImage(uri = emojiPath, modifier = Modifier.fillMaxSize())
        }
    }
}

@Stable
data object RichImageDrawer : RichDrawer {
    override val type: String = RichType.Image.value

    override fun RichRenderScope.render(item: RichObject) = item.cast<RichNodeImage> {
        renderCompose(item.width, item.height) {
            WebImage(
                uri = item.uri,
                quality = ImageQuality.Low,
                modifier = Modifier.padding(horizontal = Theme.padding.g * item.width / 2).fillMaxSize()
            )
        }
    }
}

@Stable
val RachelRichParser by lazy { RichParser.Default }

@Composable
fun RachelRichText(
    text: RichString,
    modifier: Modifier = Modifier,
    onLinkClick: ((String) -> Unit)? = null,
    onTopicClick: ((String) -> Unit)? = null,
    onAtClick: ((String) -> Unit)? = null,
    style: TextStyle = LocalStyle.current,
    color: Color = Colors.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    fixLineHeight: Boolean = false
) {
    val onLinkClickUpdate by rememberUpdatedState(onLinkClick)
    val onTopicClickUpdate by rememberUpdatedState(onTopicClick)
    val onAtClickUpdate by rememberUpdatedState(onAtClick)

    val renderer = rememberRichRenderer(
        drawerProvider = { listOf(RichEmojiDrawer, RichImageDrawer) }
    ) {
        when (it) {
            is RichNodeLink -> onLinkClickUpdate?.invoke(it.uri)
            is RichNodeTopic -> onTopicClickUpdate?.invoke(it.uri)
            is RichNodeAt -> onAtClickUpdate?.invoke(it.uri)
        }
    }

    RichText(
        text = text,
        modifier = modifier,
        renderer = renderer,
        style = style,
        color = color,
        overflow = overflow,
        maxLines = maxLines,
        fixLineHeight = fixLineHeight
    )
}