package love.yinlin.page

import androidx.compose.foundation.layout.Arrangement
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
                        Input( maxLines = Int.MAX_VALUE)
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
                        Input(style = Theme.typography.v4.bold)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "加粗")
                    Switch(isBold, { isBold = it })
                    Text(text = "斜体")
                    Switch(isItalic, { isItalic = it })
                    Text(text = "下划线")
                    Switch(isUnderline, { isUnderline = it })
                    Text(text = "删除线")
                    Switch(isStrikethrough, { isStrikethrough = it })
                }

                val styles = listOf(
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
                )

                styles.fastForEachIndexed { index, style ->
                    var textStyle = style
                    if (isBold) textStyle = textStyle.bold
                    if (isItalic) textStyle = textStyle.copy(fontStyle = FontStyle.Italic)
                    var decoration = TextDecoration.None
                    if (isUnderline) decoration += TextDecoration.Underline
                    if (isStrikethrough) decoration += TextDecoration.LineThrough
                    textStyle = textStyle.copy(textDecoration = decoration)

                    SelectionBox {
                        Text(
                            text = "[v${index + 1}] $text",
                            style = textStyle
                        )
                    }
                }
            }

            Component("StrokeText") {
                StrokeText(text = text, strokeColor = Theme.color.primary, style = Theme.typography.v2)
            }

            Component("RichText") {
                val fontSize = LocalStyle.current.fontSize * 2f
                val emojiList = listOf(Icons.Home, Icons.Clear, Icons.Token)

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

                // 仅作解码测试, 实际可以直接使用 richText
                val decodeText = remember { RichParser.Default.parse(encodedText) }

                val renderer = rememberRichRenderer({
                    listOf(
                        object : RichDrawer {
                            override val type: String = RichType.Emoji.value
                            override fun RichRenderScope.render(item: RichObject) = item.cast<RichNodeEmoji> {
                                renderCompose {
                                    Icon(icon = emojiList[item.id], modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    )
                })

                ExampleRow {
                    Example("RichText") {
                        SelectionBox {
                            RichText(text = decodeText, renderer = renderer)
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