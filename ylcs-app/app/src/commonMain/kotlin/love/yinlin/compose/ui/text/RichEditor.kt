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
import androidx.compose.ui.text.input.ImeAction
import love.yinlin.compose.*
import love.yinlin.data.rachel.emoji.Emoji
import love.yinlin.data.rachel.emoji.EmojiType
import love.yinlin.compose.ui.container.TabBar
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.ClickText
import love.yinlin.compose.ui.layout.ActionScope

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
    private val inputState = TextInputState()
    private val focusRequester = FocusRequester()

    private var enablePreview by mutableStateOf(false)
    private var currentPage by mutableStateOf(RichEditorPage.CONTENT)

    protected open val useEmoji: Boolean = true
    protected open val useImage: Boolean = false
    protected open val useLink: Boolean = true
    protected open val useTopic: Boolean = true
    protected open val useAt: Boolean = false

    private var emojiClassify by mutableStateOf(EmojiType.Static)

    val richString: RichString get() = RichEditorParser.parse(inputState.value.text)
    var text: String get() = inputState.value.text
        set(value) { inputState.text = value }
    val ok: Boolean by derivedStateOf { inputState.ok }

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
                currentPage = emojiClassify.ordinal,
                onNavigate = { emojiClassify = EmojiType.fromInt(it) },
                items = remember { EmojiType.entries.map { it.title } },
                modifier = Modifier.fillMaxWidth()
            )

            val items = remember(emojiClassify) {
                (emojiClassify.start .. emojiClassify.end).map { Emoji(it, emojiClassify) }
            }

            LazyVerticalGrid(
                columns = GridCells.FixedSize(CustomTheme.size.icon),
                contentPadding = CustomTheme.padding.equalValue,
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
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
        val title = rememberTextInputState()
        val link = rememberTextInputState()
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextInput(
                    state = title,
                    hint = "标题",
                    maxLength = 16,
                    modifier = Modifier.weight(1f)
                )
                ClickText(
                    text = "插入",
                    icon = Icons.Outlined.InsertLink,
                    enabled = title.ok && link.ok,
                    onClick = { closeLayout("[lk|${link.text}|${title.text}]") }
                )
            }
            TextInput(
                state = link,
                hint = "链接",
                maxLength = 256,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    protected open fun TopicLayout(modifier: Modifier) {
        val title = rememberTextInputState()
        val topic = rememberTextInputState()
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextInput(
                    state = title,
                    hint = "标题",
                    maxLength = 16,
                    modifier = Modifier.weight(1f)
                )
                ClickText(
                    text = "插入",
                    icon = Icons.Outlined.Tag,
                    enabled = title.ok && topic.ok,
                    onClick = { closeLayout("[tp|${topic.text}|#${title.text}#]") }
                )
            }
            TextInput(
                state = topic,
                hint = "链接",
                maxLength = 32,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    protected open fun AtLayout(modifier: Modifier) {}

    @Composable
    private fun InputLayout(
        hint: String?,
        maxLength: Int,
        imeAction: ImeAction,
        onImeClick: (KeyboardActionScope.() -> Unit)?,
        modifier: Modifier = Modifier
    ) {
        TextInput(
            state = inputState,
            hint = hint,
            maxLength = maxLength,
            maxLines = 10,
            minLines = if (enablePreview) 10 else 1,
            leadingIcon = {
                ClickIcon(
                    icon = Icons.Outlined.Preview,
                    color = if (enablePreview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    onClick = { enablePreview = !enablePreview }
                )
            },
            clearButton = false,
            imeAction = imeAction,
            onImeClick = onImeClick,
            modifier = modifier.focusRequester(focusRequester)
        )
    }

    @Composable
    private fun PreviewText(modifier: Modifier = Modifier) {
        Box(modifier = modifier) {
            when (currentPage) {
                RichEditorPage.CONTENT -> {
                    Box(modifier = Modifier.fillMaxSize()
                        .border(width = CustomTheme.border.medium, color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.extraSmall)
                        .padding(OutlinedTextFieldDefaults.contentPadding())
                    ) {
                        RichText(
                            text = remember(inputState.value.text) { richString },
                            maxLines = Int.MAX_VALUE,
                            canSelected = false,
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
        maxLength: Int,
        imeAction: ImeAction,
        onImeClick: (KeyboardActionScope.() -> Unit)?,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            PreviewText(modifier = Modifier.fillMaxWidth().aspectRatio(2f))
            InputLayout(
                hint = hint,
                maxLength = maxLength,
                imeAction = imeAction,
                onImeClick = onImeClick,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f)
            )
        }
    }

    @Composable
    private fun LandscapePreviewLayout(
        hint: String?,
        maxLength: Int,
        imeAction: ImeAction,
        onImeClick: (KeyboardActionScope.() -> Unit)?,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InputLayout(
                hint = hint,
                maxLength = maxLength,
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
        maxLength: Int,
        imeAction: ImeAction,
        onImeClick: (KeyboardActionScope.() -> Unit)?,
        modifier: Modifier = Modifier
    ) {
        if (LocalDevice.current.type == Device.Type.PORTRAIT) {
            PortraitPreviewLayout(
                hint = hint,
                maxLength = maxLength,
                imeAction = imeAction,
                onImeClick = onImeClick,
                modifier = modifier
            )
        }
        else {
            LandscapePreviewLayout(
                hint = hint,
                maxLength = maxLength,
                imeAction = imeAction,
                onImeClick = onImeClick,
                modifier = modifier
            )
        }
    }

    @Composable
    fun Content(
        hint: String?,
        maxLength: Int,
        imeAction: ImeAction,
        onImeClick: (KeyboardActionScope.() -> Unit)?,
        modifier: Modifier = Modifier
    ) {
        LaunchedEffect(enablePreview) {
            currentPage = RichEditorPage.CONTENT
        }

        Surface(modifier = modifier) {
            if (enablePreview) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                ) {
                    ActionScope.Right.ActionLayout(modifier = Modifier.fillMaxWidth()) {
                        if (useEmoji) {
                            val isActive = currentPage == RichEditorPage.EMOJI
                            Action(
                                icon = Icons.Outlined.AddReaction,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ) {
                                currentPage = if (isActive) RichEditorPage.CONTENT else RichEditorPage.EMOJI
                            }
                        }
                        if (useImage) {
                            val isActive = currentPage == RichEditorPage.IMAGE
                            Action(
                                icon = Icons.Outlined.AddPhotoAlternate,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ) {
                                currentPage = if (isActive) RichEditorPage.CONTENT else RichEditorPage.IMAGE
                            }
                        }
                        if (useLink) {
                            val isActive = currentPage == RichEditorPage.LINK
                            Action(
                                icon = Icons.Outlined.InsertLink,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ) {
                                currentPage = if (isActive) RichEditorPage.CONTENT else RichEditorPage.LINK
                            }
                        }
                        if (useTopic) {
                            val isActive = currentPage == RichEditorPage.TOPIC
                            Action(
                                icon = Icons.Outlined.Tag,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ) {
                                currentPage = if (isActive) RichEditorPage.CONTENT else RichEditorPage.TOPIC
                            }
                        }
                        if (useAt) {
                            val isActive = currentPage == RichEditorPage.AT
                            Action(
                                icon = Icons.Outlined.AlternateEmail,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ) {
                                currentPage = if (isActive) RichEditorPage.CONTENT else RichEditorPage.AT
                            }
                        }
                    }
                    PreviewLayout(
                        hint = hint,
                        maxLength = maxLength,
                        imeAction = imeAction,
                        onImeClick = onImeClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            else {
                InputLayout(
                    hint = hint,
                    maxLength = maxLength,
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
    maxLength: Int = 0,
    imeAction: ImeAction = ImeAction.Done,
    onImeClick: (KeyboardActionScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) = state.Content(
    hint = hint,
    maxLength = maxLength,
    imeAction = imeAction,
    onImeClick = onImeClick,
    modifier = modifier
)