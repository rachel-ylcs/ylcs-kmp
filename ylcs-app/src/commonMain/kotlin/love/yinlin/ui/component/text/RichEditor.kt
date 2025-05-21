package love.yinlin.ui.component.text

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import love.yinlin.common.Device
import love.yinlin.common.EmojiManager
import love.yinlin.common.LocalDevice
import love.yinlin.common.ThemeValue
import love.yinlin.extension.rememberState
import love.yinlin.extension.rememberValueState
import love.yinlin.ui.component.container.TabBar
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
                    index = length
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

    var emojiClassify by mutableIntStateOf(0)

    @Composable
    open fun EmojiLayout(modifier: Modifier, focusRequester: FocusRequester, onClose: (String?) -> Unit) {
        Column(modifier = modifier) {
            TabBar(
                currentPage = emojiClassify,
                onNavigate = { emojiClassify = it },
                items = remember { EmojiManager.classifyMap.map { it.title } },
                modifier = Modifier.fillMaxWidth()
            )
            LazyVerticalGrid(
                columns = GridCells.FixedSize(ThemeValue.Size.Icon),
                contentPadding = ThemeValue.Padding.EqualValue,
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(
                    items = EmojiManager.classifyMap[emojiClassify].items,
                    key = { it.id }
                ) { emoji ->
                    MiniImage(
                        painter = painterResource(emoji.res),
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable {
                            onClose("[em|${emoji.id}]")
                        }
                    )
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
        val focusRequester = remember { FocusRequester() }
        when (currentPage) {
            RichEditorPage.CONTENT -> {
                TextInput(
                    state = inputState,
                    maxLength = maxLength,
                    maxLines = if (enablePreview) Int.MAX_VALUE else 5,
                    minLines = if (enablePreview) Int.MAX_VALUE else 1,
                    clearButton = false,
                    modifier = layoutModifier.focusRequester(focusRequester)
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
            RichEditorPage.EMOJI -> EmojiLayout(layoutModifier, focusRequester, onClose)
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