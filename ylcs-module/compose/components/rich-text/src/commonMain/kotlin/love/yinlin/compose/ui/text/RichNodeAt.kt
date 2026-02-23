package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.withLink
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import love.yinlin.compose.Colors
import love.yinlin.extension.Int
import love.yinlin.extension.JsonObjectScope
import love.yinlin.extension.String

@Stable
class RichNodeAt internal constructor(
    val uri: String,
    val text: String,
    val color: Color?,
) : RichValue() {
    override val type: String = RichType.At.value

    override fun JsonObjectScope.children() {
        RichArg.Uri.value with uri
        RichArg.Text.value with text
        if (color != null) RichArg.Color.value with color.toArgb()
    }

    @Stable
    data object Drawer : RichDrawer {
        override val type: String = RichType.Link.value

        override fun RichRenderScope.render(item: RichObject) = item.cast<RichNodeAt> {
            builder.append(' ')
            builder.withLink(LinkAnnotation.Url(
                url = item.uri,
                styles = TextLinkStyles(style = SpanStyle(color = item.color ?: Colors.Red4)),
                linkInteractionListener = { onAction(item) }
            )) {
                append(item.text)
            }
            builder.append(' ')
        }
    }

    @Stable
    data object Converter : RichConverter {
        override val type: String = RichType.At.value

        override fun RichParseScope.convert(list: RichList, json: JsonElement) = json.cast<JsonObject> {
            list.at(
                uri = json[RichArg.Uri.value].String,
                text = json[RichArg.Text.value].String,
                color = json[RichArg.Color.value]?.Int?.let { Colors(it) }
            )
        }
    }
}

fun RichList.at(uri: String, text: String, color: Color? = null) = addNode(RichNodeAt(uri, text, color))