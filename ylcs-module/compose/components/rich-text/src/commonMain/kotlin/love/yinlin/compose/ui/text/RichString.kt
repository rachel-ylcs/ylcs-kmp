package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import love.yinlin.extension.Array
import love.yinlin.extension.JsonObjectScope

@Stable
class RichString @PublishedApi internal constructor(): RichList() {
    override val type: String = RichType.Root.value

    override fun JsonObjectScope.children() {}

    @Stable
    data object Converter : RichConverter {
        override val type: String = RichType.Root.value

        override fun RichParseScope.convert(list: RichList, json: JsonElement) = json.cast<JsonObject> {
            for (item in json[RichArg.Member.value].Array) {
                parseIn(list, item)
            }
        }
    }
}

inline fun buildRichString(content: RichList.() -> Unit): RichString {
    val richString = RichString()
    richString.content()
    return richString
}