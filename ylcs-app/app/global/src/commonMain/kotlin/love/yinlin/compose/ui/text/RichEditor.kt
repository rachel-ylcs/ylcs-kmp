package love.yinlin.compose.ui.text

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.util.fastForEachIndexed
import love.yinlin.compose.Device
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.data.rachel.emoji.Emoji
import love.yinlin.data.rachel.emoji.EmojiType
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.navigation.TabBar

@Stable
private object RichEditorParser {
    private fun RichList.parseStyle(args: List<String>): Boolean {
        val type = args.getOrNull(0) ?: return false
        when (type) {
            RichType.Emoji.value -> emoji(
                id = args.getOrNull(1)?.toIntOrNull() ?: return false
            )
            RichType.Image.value -> image(
                uri = args.getOrNull(1) ?: return false,
                width = args.getOrNull(2)?.toFloatOrNull() ?: 1f,
                height = args.getOrNull(3)?.toFloatOrNull() ?: 1f
            )
            RichType.Link.value -> link(
                uri = args.getOrNull(1) ?: return false,
                text = args.getOrNull(2) ?: return false
            )
            RichType.Topic.value -> topic(
                uri = args.getOrNull(1) ?: return false,
                text = args.getOrNull(2) ?: return false
            )
            RichType.At.value -> at(
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
enum class RichEditorPage(val icon: ImageVector) {
    CONTENT(Icons.TextFields),
    EMOJI(Icons.AddReaction),
    IMAGE(Icons.AddPhotoAlternate),
    LINK(Icons.InsertLink),
    TOPIC(Icons.Tag),
    AT(Icons.AlternateEmail);
}

@Stable
open class RichEditorState(maxLength: Int) {
    private val inputState = InputState(maxLength = maxLength)
    private val focusRequester = FocusRequester()

    private var enablePreview by mutableStateOf(false)
    private var currentPage by mutableStateOf(RichEditorPage.CONTENT)

    protected open val useEmoji: Boolean = true
    protected open val useImage: Boolean = false
    protected open val useLink: Boolean = true
    protected open val useTopic: Boolean = true
    protected open val useAt: Boolean = false

    private var emojiClassify by mutableStateOf(EmojiType.Static)

    val richString: RichString get() = RichEditorParser.parse(inputState.text)
    var text: String get() = inputState.text
        set(value) { inputState.text = value }
    val isSafe: Boolean get() = inputState.isSafe

    protected fun closeLayout(result: String?) {
        if (!result.isNullOrEmpty()) inputState.insert(result)
        focusRequester.requestFocus()
        currentPage = RichEditorPage.CONTENT
    }

    fun closePreview() { enablePreview = false }

    @Composable
    protected open fun EmojiLayout(modifier: Modifier) {
        Column(modifier = modifier) {
            TabBar(
                size = EmojiType.entries.size,
                index = emojiClassify.ordinal,
                onNavigate = { emojiClassify = EmojiType.entries[it] },
                titleProvider = { EmojiType.entries[it].title },
                modifier = Modifier.fillMaxWidth()
            )

            val items = remember(emojiClassify) {
                (emojiClassify.start .. emojiClassify.end).map { Emoji(it, emojiClassify) }
            }

            LazyVerticalGrid(
                columns = GridCells.FixedSize(Theme.size.icon),
                contentPadding = Theme.padding.eValue,
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.e),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(
                    items = items,
                    key = { it.id }
                ) { emoji ->
                    WebImage(
                        uri = remember(emoji) { emoji.previewPath },
                        animated = false,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable {
                            closeLayout("[em|${emoji.id}]")
                        }
                    )
                }
            }
        }
    }

    @Composable
    protected open fun ImageLayout(modifier: Modifier) {}

    @Composable
    protected open fun LinkLayout(modifier: Modifier) {
        val title = rememberInputState(maxLength = 16)
        val link = rememberInputState(maxLength = 256)
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Input(
                    state = title,
                    hint = "标题",
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = "插入",
                    icon = Icons.InsertLink,
                    enabled = title.isSafe && link.isSafe,
                    onClick = { closeLayout("[lk|${link.text}|${title.text}]") }
                )
            }
            Input(
                state = link,
                hint = "链接",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    protected open fun TopicLayout(modifier: Modifier) {
        val title = rememberInputState(maxLength = 16)
        val topic = rememberInputState(maxLength = 32)
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Input(
                    state = title,
                    hint = "标题",
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = "插入",
                    icon = Icons.Tag,
                    enabled = title.isSafe && topic.isSafe,
                    onClick = { closeLayout("[tp|${topic.text}|#${title.text}#]") }
                )
            }
            Input(
                state = topic,
                hint = "链接",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    protected open fun AtLayout(modifier: Modifier) {}

    @Composable
    private fun InputLayout(
        hint: String?,
        imeAction: ImeAction,
        onImeClick: (KeyboardActionScope.() -> Unit)?,
        modifier: Modifier = Modifier
    ) {
        val iconColor by rememberUpdatedState(if (enablePreview) Theme.color.primary else LocalColor.current)
        Input(
            state = inputState,
            hint = hint,
            maxLines = 10,
            minLines = if (enablePreview) 10 else 1,
            imeAction = imeAction,
            onImeClick = onImeClick,
            leading = remember { InputDecoration.Icon(
                icon = { Icons.Preview },
                color = { iconColor },
                onClick = { enablePreview = !enablePreview }
            ) },
            modifier = modifier.focusRequester(focusRequester)
        )
    }

    @Composable
    private fun PreviewText(modifier: Modifier = Modifier) {
        Box(modifier = modifier) {
            when (currentPage) {
                RichEditorPage.CONTENT -> {
                    Box(modifier = Modifier.fillMaxSize()
                        .border(width = Theme.border.v5, color = Theme.color.secondaryContainer, shape = Theme.shape.v10)
                        .padding(Theme.padding.value9)
                    ) {
                        RachelRichText(
                            text = remember(inputState.text) { richString },
                            maxLines = Int.MAX_VALUE,
                            fixLineHeight = true,
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        )
                    }
                }
                RichEditorPage.EMOJI -> EmojiLayout(modifier = Modifier.fillMaxSize())
                RichEditorPage.IMAGE -> ImageLayout(modifier = Modifier.fillMaxSize())
                RichEditorPage.LINK -> LinkLayout(modifier = Modifier.fillMaxSize())
                RichEditorPage.TOPIC -> TopicLayout(modifier = Modifier.fillMaxSize())
                RichEditorPage.AT -> AtLayout(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Composable
    private fun PortraitPreviewLayout(
        hint: String?,
        imeAction: ImeAction,
        onImeClick: (KeyboardActionScope.() -> Unit)?,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            PreviewText(modifier = Modifier.fillMaxWidth().aspectRatio(2f))
            InputLayout(
                hint = hint,
                imeAction = imeAction,
                onImeClick = onImeClick,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f)
            )
        }
    }

    @Composable
    private fun LandscapePreviewLayout(
        hint: String?,
        imeAction: ImeAction,
        onImeClick: (KeyboardActionScope.() -> Unit)?,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InputLayout(
                hint = hint,
                imeAction = imeAction,
                onImeClick = onImeClick,
                modifier = Modifier.weight(1f).aspectRatio(2f)
            )
            PreviewText(modifier = Modifier.weight(1f).aspectRatio(2f))
        }
    }

    @Composable
    private fun PreviewLayout(
        hint: String?,
        imeAction: ImeAction,
        onImeClick: (KeyboardActionScope.() -> Unit)?,
        modifier: Modifier = Modifier
    ) {
        if (LocalDevice.current.type == Device.Type.PORTRAIT) {
            PortraitPreviewLayout(
                hint = hint,
                imeAction = imeAction,
                onImeClick = onImeClick,
                modifier = modifier
            )
        }
        else {
            LandscapePreviewLayout(
                hint = hint,
                imeAction = imeAction,
                onImeClick = onImeClick,
                modifier = modifier
            )
        }
    }

    @Composable
    fun Content(
        hint: String?,
        imeAction: ImeAction,
        onImeClick: (KeyboardActionScope.() -> Unit)?,
        modifier: Modifier = Modifier
    ) {
        LaunchedEffect(enablePreview) {
            currentPage = RichEditorPage.CONTENT
        }

        Box(modifier = modifier) {
            if (enablePreview) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    ActionScope.Right.Container(modifier = Modifier.fillMaxWidth()) {
                        val enableList by rememberDerivedState { listOf(true, useEmoji, useImage, useLink, useTopic, useAt) }
                        RichEditorPage.entries.fastForEachIndexed { index, editorPage ->
                            val isActive = currentPage == editorPage
                            Icon(
                                icon = editorPage.icon,
                                color = if (isActive) Theme.color.primary else LocalColor.current,
                                enabled = enableList[index],
                                onClick = { currentPage = if (isActive) RichEditorPage.CONTENT else editorPage }
                            )
                        }
                    }
                    PreviewLayout(
                        hint = hint,
                        imeAction = imeAction,
                        onImeClick = onImeClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            else {
                InputLayout(
                    hint = hint,
                    imeAction = imeAction,
                    onImeClick = onImeClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun RichEditor(
    state: RichEditorState,
    hint: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeClick: (KeyboardActionScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) = state.Content(
    hint = hint,
    imeAction = imeAction,
    onImeClick = onImeClick,
    modifier = modifier
)