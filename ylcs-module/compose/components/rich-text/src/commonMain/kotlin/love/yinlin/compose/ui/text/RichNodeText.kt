package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import love.yinlin.extension.json

@Stable
class RichNodeText internal constructor(val text: String) : RichObject {
    override val type: String = RichType.Text.value
    override val json: JsonElement = text.json

    @Stable
    data object Drawer : RichDrawer {
        override val type: String = RichType.Text.value

        override fun RichRenderScope.render(item: RichObject) = item.cast<RichNodeText> {
            builder.append(item.text)
        }
    }

    @Stable
    data object Converter : RichConverter {
        override val type: String = RichType.Text.value

        override fun RichParseScope.convert(list: RichList, json: JsonElement) = json.cast<JsonPrimitive> {
            list.text(json.content)
        }
    }
}

fun RichList.text(str: String) = addNode(RichNodeText(str))