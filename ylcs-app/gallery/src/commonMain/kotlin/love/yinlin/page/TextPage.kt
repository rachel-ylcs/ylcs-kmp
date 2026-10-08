package love.yinlin.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.util.fastForEachIndexed
import love.yinlin.Page
import love.yinlin.GalleryCaption
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.text.*

@Stable
object TextPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            val text = "一只敏捷的棕色狐狸跳过一只懒惰的狗\nThe quick brown fox jumps over a lazy dog"

            Component("Input") {
                ExampleRow {
                    Example("Normal", modifier = Modifier.weight(1f)) {
                        Input()
                    }
                    Example("Hint", modifier = Modifier.weight(1f)) {
                        Input(hint = "Input something")
                    }
                    Example("MultiLine", modifier = Modifier.weight(1f)) {
                        Input(maxLines = 2)
                    }
                }
                ExampleRow {
                    Example("MaxLength 16", modifier = Modifier.weight(1f)) {
                        Input(state = rememberInputState(maxLength = 16))
                    }
                    Example("ClearButton", modifier = Modifier.weight(1f)) {
                        Input(trailing = InputDecoration.Icon.Clear)
                    }
                    Example("LengthViewer", modifier = Modifier.weight(1f)) {
                        Input(
                            state = rememberInputState(maxLength = 16),
                            leading = InputDecoration.Icon(icon = { Icons.Home }),
                            trailing = InputDecoration.LengthViewer
                        )
                    }
                }
                ExampleRow {
                    Example("Style", modifier = Modifier.weight(1f)) {
                        Input(
                            style = Theme.typography.v4.bold.copy(textDecoration = TextDecoration.LineThrough)
                        )
                    }
                    Example("Readonly", modifier = Modifier.weight(1f)) {
                        Input(
                            state = rememberInputState("Readonly"),
                            enabled = false
                        )
                    }

                    val passwordState = rememberInputState(maxLength = 16)
                    Example("Password: ${passwordState.text}", modifier = Modifier.weight(1f)) {
                        PasswordInput(passwordState, hint = "enter password")
                    }
                }
            }

            Component("Text") {
                var isBold by rememberFalse()
                var isItalic by rememberFalse()
                var isUnderline by rememberFalse()
                var isStrikethrough by rememberFalse()

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)) {
                        Text(text = "加粗")
                        Switch(isBold, { isBold = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)) {
                        Text(text = "斜体")
                        Switch(isItalic, { isItalic = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)) {
                        Text(text = "下划线")
                        Switch(isUnderline, { isUnderline = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)) {
                        Text(text = "删除线")
                        Switch(isStrikethrough, { isStrikethrough = it })
                    }
                }

                val styles = [
                    Theme.typography.v1,
                    Theme.typography.v2,
                    Theme.typography.v3,
                    Theme.typography.v4,
                    Theme.typography.v5,
                    Theme.typography.v6,
                    Theme.typography.v7,
                    Theme.typography.v8,
                    Theme.typography.v9,
                    Theme.typography.v10
                ]

                styles.fastForEachIndexed { index, style ->
                    var textStyle = style
                    if (isBold) textStyle = textStyle.bold
                    if (isItalic) textStyle = textStyle.copy(fontStyle = FontStyle.Italic)
                    var decoration = TextDecoration.None
                    if (isUnderline) decoration += TextDecoration.Underline
                    if (isStrikethrough) decoration += TextDecoration.LineThrough
                    textStyle = textStyle.copy(textDecoration = decoration)

                    Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                        GalleryCaption("TYPOGRAPHY / v${index + 1}")
                        SelectionBox {
                            Text(text = text, style = textStyle)
                        }
                    }
                }
            }

            Component("StrokeText") {
                StrokeText(text = text, strokeColor = Theme.color.primary, style = Theme.typography.v2)
            }

            Component("RichText") {
                val fontSize = LocalStyle.current.fontSize * 2f
                val emojiList = [Icons.Home, Icons.Clear, Icons.Token]

                val richText = remember {
                    buildRichString {
                        text("hello, ")
                        emoji(2)
                        br()
                        style(color = Colors.Red4, bold = true, italic = true, fontSize = fontSize) {
                            text("w")
                        }
                        link("https://yinlin.love", "or")
                        emoji(0)
                        text("ld")
                        emoji(1)
                        text("!")
                    }
                }

                val encodedText = remember { richText.toString() }

                val renderer = rememberRichRenderer({
                    [
                        object : RichDrawer {
                            override val type: String = RichType.Emoji.value
                            override fun RichRenderScope.render(item: RichObject) = item.cast<RichNodeEmoji> {
                                renderCompose {
                                    Icon(icon = emojiList[item.id], modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    ]
                })

                ExampleRow {
                    Example("RichText") {
                        SelectionBox {
                            RichText(text = richText, renderer = renderer)
                        }
                    }

                    Example("EncodedText") {
                        SelectionBox {
                            Text(encodedText)
                        }
                    }
                }
            }
        }
    }
}
