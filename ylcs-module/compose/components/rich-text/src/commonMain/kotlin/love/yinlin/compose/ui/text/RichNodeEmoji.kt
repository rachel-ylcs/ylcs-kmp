package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import love.yinlin.extension.json

@RichExternal(RichDrawer::class)
@Stable
class RichNodeEmoji internal constructor(val id: Int) : RichObject {
    override val type: String = RichType.Emoji.value
    override val json: JsonElement = id.json

    @Stable
    data object Converter : RichConverter {
        override val type: String = RichType.Emoji.value

        override fun RichParseScope.convert(list: RichList, json: JsonElement) = json.cast<JsonPrimitive> {
            list.emoji(json.int)
        }
    }
}

fun RichList.emoji(id: Int) = addNode(RichNodeEmoji(id))