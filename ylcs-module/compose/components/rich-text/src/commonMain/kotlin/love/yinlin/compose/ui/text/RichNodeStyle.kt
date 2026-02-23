package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import love.yinlin.compose.Colors
import love.yinlin.extension.Array
import love.yinlin.extension.Boolean
import love.yinlin.extension.Float
import love.yinlin.extension.Int
import love.yinlin.extension.JsonObjectScope

@Stable
class RichNodeStyle @PublishedApi internal constructor(
    val fontSize: TextUnit?,
    val color: Color?,
    val bold: Boolean,
    val italic: Boolean,
    val underline: Boolean,
    val strikethrough: Boolean
) : RichList() {
    override val type: String = RichType.Style.value

    override fun JsonObjectScope.children() {
        fontSize?.let { RichArg.FontSize.value with it.value }
        color?.let { RichArg.Color.value with it.toArgb() }
        if (bold) RichArg.Bold.value with bold
        if (italic) RichArg.Italic.value with italic
        if (underline) RichArg.Underline.value with underline
        if (strikethrough) RichArg.Strikethrough.value with strikethrough
    }

    @Stable
    data object Drawer : RichDrawer {
        override val type: String = RichType.Style.value

        override fun RichRenderScope.render(item: RichObject) = item.cast<RichNodeStyle> {
            val textDecorations = mutableListOf<TextDecoration>()
            if (item.underline) textDecorations += TextDecoration.Underline
            if (item.strikethrough) textDecorations += TextDecoration.LineThrough
            builder.withStyle(SpanStyle(
                color = item.color ?: Color.Unspecified,
                fontSize = item.fontSize ?: TextUnit.Unspecified,
                fontWeight = if (item.bold) FontWeight.SemiBold else FontWeight.Light,
                fontStyle = if (item.italic) FontStyle.Italic else null,
                textDecoration = if (textDecorations.isEmpty()) TextDecoration.None else TextDecoration.combine(textDecorations)
            )) {
                renderList(item.items)
            }
        }
    }

    @Stable
    data object Converter : RichConverter {
        override val type: String = RichType.Style.value

        override fun RichParseScope.convert(list: RichList, json: JsonElement) = json.cast<JsonObject> {
            list.style(
                fontSize = json[RichArg.FontSize.value]?.Float?.sp,
                color = json[RichArg.Color.value]?.Int?.let { Colors(it) },
                bold = json[RichArg.Bold.value]?.Boolean == true,
                italic = json[RichArg.Italic.value]?.Boolean == true,
                underline = json[RichArg.Underline.value]?.Boolean == true,
                strikethrough = json[RichArg.Strikethrough.value]?.Boolean == true
            ) {
                for (item in json[RichArg.Member.value].Array) {
                    parseIn(this, item)
                }
            }
        }
    }
}

inline fun RichList.style(
    fontSize: TextUnit? = null,
    color: Color? = null,
    bold: Boolean = false,
    italic: Boolean = false,
    underline: Boolean = false,
    strikethrough: Boolean = false,
    content: RichList.() -> Unit
) = addListNode(RichNodeStyle(
    fontSize = fontSize,
    color = color,
    bold = bold,
    italic = italic,
    underline = underline,
    strikethrough = strikethrough,
), content)

