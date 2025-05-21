package love.yinlin.ui.component.text

import KottieAnimation
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kottieComposition.KottieCompositionSpec
import kottieComposition.rememberKottieComposition
import love.yinlin.common.Device
import love.yinlin.common.Emoji
import love.yinlin.common.EmojiManager
import love.yinlin.common.LocalDevice
import love.yinlin.common.ThemeValue
import love.yinlin.extension.rememberState
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.layout.ActionScope
import org.jetbrains.compose.resources.painterResource

@Stable
private object RichEditorParser {
    private fun RichContainer.parseStyle(args: List<String>): Boolean {
        val type = args.getOrNull(0) ?: return false
        when (type) {
            RICH_TYPE_EMOJI -> emoji(
                id = args.getOrNull(1)?.toIntOrNull() ?: return false
            )
            RICH_TYPE_IMAGE -> image(
                uri = args.getOrNull(1) ?: return false,
                width = args.getOrNull(2)?.toFloatOrNull() ?: 1f,
                height = args.getOrNull(3)?.toFloatOrNull() ?: 1f
            )
            RICH_TYPE_LINK -> link(
                uri = args.getOrNull(1) ?: return false,
                text = args.getOrNull(2) ?: return false
            )
            RICH_TYPE_TOPIC -> topic(
                uri = args.getOrNull(1) ?: return false,
                text = args.getOrNull(2) ?: return false
            )
            RICH_TYPE_AT -> at(
                uri = args.getOrNull(1) ?: return false,
                text = args.getOrNull(2) ?: return false
            )
            else -> return false
        }
        return true
    }

    fun parse(data: String): RichString = buildRichString {
        var index = 0
        val length = data.length
        val currentNormal = StringBuilder()
        while (index < length) {
            val normalStart = index
            while (index < length && data[index] != '[') index++
            if (index > normalStart) currentNormal.append(data, normalStart, index)
            if (index < length && data[index] == '[') {
                val markStart = index
                index++
                val contentStart = index
                var found = false
                while (index < length) {
                    if (data[index] == ']') {
                        found = true
                        break
                    }
                    index++
                }
                if (found) {
                    if (currentNormal.isNotEmpty()) {
                        text(currentNormal.toString())
                        currentNormal.clear()
                    }
                    val content = data.substring(contentStart, index)
                    if (!parseStyle(content.split("|"))) text(content)
                    index++
                }
                else {
                    currentNormal.append(data.substring(markStart, length))
                    index = length // 结束循环
                }
            }
        }
        if (currentNormal.isNotEmpty()) text(currentNormal.toString())
    }
}

@Stable
enum class RichEditorPage {
    CONTENT, EMOJI, IMAGE, LINK, TOPIC, AT
}

@Stable
open class RichEditorState {
    val inputState = TextInputState()

    var enablePreview by mutableStateOf(false)

    open val useEmoji: Boolean get() = true
    open val useImage: Boolean get() = false
    open val useLink: Boolean get() = true
    open val useTopic: Boolean get() = false
    open val useAt: Boolean get() = false

    @Composable
    open fun EmojiLayout(modifier: Modifier, onClose: (String?) -> Unit) {
        val emojiMap = remember { EmojiManager.toList() }

        LazyVerticalGrid(
            columns = GridCells.FixedSize(ThemeValue.Size.Icon),
            horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
            modifier = modifier
        ) {
            items(
                items = emojiMap,
                key = { it.id }
            ) { emoji ->
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable {
                    onClose("[em|${emoji.id}]")
                }) {
                    when (emoji) {
                        is Emoji.Image -> {
                            MiniImage(
                                painter = painterResource(emoji.res),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        is Emoji.Lottie -> {
                            KottieAnimation(
                                composition = rememberKottieComposition(spec = KottieCompositionSpec.JsonString(emoji.data)),
                                progress = { 0f },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    open fun ImageLayout(modifier: Modifier, onClose: (String?) -> Unit) {}

    @Composable
    open fun LinkLayout(modifier: Modifier, onClose: (String?) -> Unit) {

    }

    @Composable
    open fun TopicLayout(modifier: Modifier, onClose: (String?) -> Unit) {}

    @Composable
    open fun AtLayout(modifier: Modifier, onClose: (String?) -> Unit) {}

    @Composable
    fun RichEditorContent(
        currentPage: RichEditorPage,
        onClose: (String?) -> Unit,
        maxLength: Int = 0,
        modifier: Modifier = Modifier
    ) {
        val layoutModifier = if (enablePreview) modifier.aspectRatio(2.5f) else modifier
        when (currentPage) {
            RichEditorPage.CONTENT -> {
                TextInput(
                    state = inputState,
                    maxLength = maxLength,
                    maxLines = if (enablePreview) Int.MAX_VALUE else 5,
                    minLines = if (enablePreview) Int.MAX_VALUE else 1,
                    clearButton = false,
                    modifier = layoutModifier
                )
            }
            RichEditorPage.EMOJI -> EmojiLayout(layoutModifier, onClose)
            RichEditorPage.IMAGE -> ImageLayout(layoutModifier, onClose)
            RichEditorPage.LINK -> LinkLayout(layoutModifier, onClose)
            RichEditorPage.TOPIC -> TopicLayout(layoutModifier, onClose)
            RichEditorPage.AT -> AtLayout(layoutModifier, onClose)
        }
        if (enablePreview) {
            Box(modifier = layoutModifier
                .border(width = ThemeValue.Border.Medium, color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.extraSmall)
                .padding(OutlinedTextFieldDefaults.contentPadding())
                .verticalScroll(rememberScrollState())
            ) {
                RichText(
                    text = remember(inputState.value.text) { RichEditorParser.parse(inputState.value.text) },
                    maxLines = Int.MAX_VALUE,
                    canSelected = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun RichEditor(
    state: RichEditorState,
    maxLength: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
        ) {
            var currentPage by rememberState { RichEditorPage.CONTENT }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClickIcon(
                    icon = Icons.Outlined.Preview,
                    color = if (state.enablePreview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        state.enablePreview = !state.enablePreview
                        currentPage = RichEditorPage.CONTENT
                    }
                )
                if (state.enablePreview) ActionScope.Right.ActionLayout(modifier = Modifier.weight(1f)) {
                    if (state.useEmoji) Action(Icons.Outlined.AddReaction) {
                        currentPage = if (currentPage == RichEditorPage.EMOJI) RichEditorPage.CONTENT else RichEditorPage.EMOJI
                    }
                    if (state.useImage) Action(Icons.Outlined.AddPhotoAlternate) {
                        currentPage = if (currentPage == RichEditorPage.IMAGE) RichEditorPage.CONTENT else RichEditorPage.IMAGE
                    }
                    if (state.useLink) Action(Icons.Outlined.Link) {
                        currentPage = if (currentPage == RichEditorPage.LINK) RichEditorPage.CONTENT else RichEditorPage.LINK
                    }
                    if (state.useTopic) Action(Icons.Outlined.Tag) {
                        currentPage = if (currentPage == RichEditorPage.TOPIC) RichEditorPage.CONTENT else RichEditorPage.TOPIC
                    }
                    if (state.useAt) Action(Icons.Outlined.AlternateEmail) {
                        currentPage = if (currentPage == RichEditorPage.AT) RichEditorPage.CONTENT else RichEditorPage.AT
                    }
                }
            }

            if (LocalDevice.current.type == Device.Type.PORTRAIT) {
                state.RichEditorContent(
                    currentPage = currentPage,
                    onClose = { result ->
                        if (!result.isNullOrEmpty()) state.inputState.insert(result)
                        currentPage = RichEditorPage.CONTENT
                    },
                    maxLength = maxLength,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace)
                ) {
                    state.RichEditorContent(
                        currentPage = currentPage,
                        onClose = { result ->
                            if (!result.isNullOrEmpty()) state.inputState.insert(result)
                            currentPage = RichEditorPage.CONTENT
                        },
                        maxLength = maxLength,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}